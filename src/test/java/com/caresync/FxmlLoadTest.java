package com.caresync;

import com.caresync.models.Role;
import com.caresync.models.User;
import com.caresync.utils.SessionManager;
import javafx.application.Platform;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class FxmlLoadTest {
    @BeforeAll
    static void startToolkit() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyStarted) {
            started.countDown();
        }
        assertTrue(started.await(10, TimeUnit.SECONDS), "JavaFX toolkit did not start.");
    }

    @Test
    void loadsApplicationScreens() throws InterruptedException {
        CountDownLatch loaded = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                User admin = new User();
                admin.setId(1);
                admin.setFullName("Clinic Administrator");
                admin.setUsername("admin");
                admin.setRole(Role.ADMIN);
                admin.setActive(true);
                SessionManager.setCurrentUser(admin);

                for (String fxml : screenNames()) {
                    Parent parent = Main.loadFXML(fxml);
                    assertNotNull(parent, fxml + " should load.");
                }
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                loaded.countDown();
            }
        });

        assertTrue(loaded.await(15, TimeUnit.SECONDS), "FXML loading did not finish.");
        if (failure.get() != null) {
            fail(failure.get());
        }
    }

    private String[] screenNames() {
        return new String[]{
                "startup_splash",
                "login",
                "loading",
                "change_password",
                "dashboard_shell",
                "admin_dashboard",
                "doctor_dashboard",
                "receptionist_dashboard",
                "patient_dashboard",
                "user_management",
                "doctor_management",
                "receptionist_management",
                "patient_management",
                "appointment_management",
                "medical_record",
                "audit_logs",
                "settings"
        };
    }
}
