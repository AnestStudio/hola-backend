package org.anest.hola.modules.identity.service;

import org.anest.hola.common.exception.AppException;
import org.anest.hola.common.exception.ErrorCode;
import org.anest.hola.modules.identity.entity.User;
import org.anest.hola.modules.identity.model.UserStatus;
import org.anest.hola.modules.identity.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class UserLoginAttemptConcurrencyTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void recordFailedLoginAttempt_UnderConcurrentRequests_ShouldNotLoseIncrementsAndShouldLockOnFifthAttempt() throws Exception {
        User savedUser = userRepository.save(User.builder()
                .email("concurrent@hola.edu.vn")
                .passwordHash("hashed-password")
                .fullName("Concurrent Test User")
                .status(UserStatus.ACTIVE)
                .failedLoginAttempts(0)
                .build());

        int threadCount = UserService.MAX_FAILED_LOGIN_ATTEMPTS;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        LocalDateTime now = LocalDateTime.of(2026, 10, 31, 16, 0);

        List<Future<Boolean>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    readyLatch.countDown();
                    assertThat(startLatch.await(5, TimeUnit.SECONDS)).isTrue();
                    return userService.recordFailedLoginAttempt(savedUser.getId(), now);
                }));
            }

            assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
            startLatch.countDown();

            List<Boolean> lockResults = new ArrayList<>();
            for (Future<Boolean> future : futures) {
                lockResults.add(future.get(10, TimeUnit.SECONDS));
            }

            assertThat(lockResults).contains(true);
            assertThat(lockResults.stream().filter(Boolean::booleanValue).count()).isEqualTo(1L);
        } finally {
            executor.shutdownNow();
        }

        User updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(updatedUser.getFailedLoginAttempts()).isEqualTo(UserService.MAX_FAILED_LOGIN_ATTEMPTS);
        assertThat(updatedUser.getLockedUntil()).isEqualTo(now.plusMinutes(UserService.LOCK_DURATION_MINUTES));
    }

    @Test
    void completeSuccessfulLogin_WhenAnotherRequestLocksAccountAfterInitialRead_ShouldReject() throws Exception {
        User savedUser = userRepository.save(User.builder()
                .email("race@hola.edu.vn")
                .passwordHash("hashed-password")
                .fullName("Race Test User")
                .status(UserStatus.ACTIVE)
                .failedLoginAttempts(4)
                .build());
        LocalDateTime now = LocalDateTime.of(2026, 10, 31, 16, 0);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            transactionTemplate.executeWithoutResult(status -> {
                assertThat(userService.findAuthByEmail(savedUser.getEmail())).isPresent();
                Future<Boolean> failedLogin = executor.submit(
                        () -> userService.recordFailedLoginAttempt(savedUser.getId(), now));
                try {
                    assertThat(failedLogin.get(10, TimeUnit.SECONDS)).isTrue();
                } catch (Exception ex) {
                    throw new AssertionError("Concurrent failed login did not complete", ex);
                }

                assertThatThrownBy(() -> userService.completeSuccessfulLogin(savedUser.getId(), now.plusSeconds(1)))
                        .isInstanceOf(AppException.class)
                        .extracting(ex -> ((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_TEMPORARILY_LOCKED);
            });
        } finally {
            executor.shutdownNow();
        }

        User updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(updatedUser.getFailedLoginAttempts()).isEqualTo(UserService.MAX_FAILED_LOGIN_ATTEMPTS);
        assertThat(updatedUser.getLockedUntil()).isEqualTo(now.plusMinutes(UserService.LOCK_DURATION_MINUTES));
    }
}

