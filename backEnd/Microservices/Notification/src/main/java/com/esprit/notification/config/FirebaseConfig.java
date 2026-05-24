package com.esprit.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@Profile("!test")
@ConditionalOnProperty(name = "notification.firebase.enabled", havingValue = "true")
public class FirebaseConfig {

    private static final String[] KNOWN_CREDENTIAL_FILENAMES = {
        "notificationsystem-3bd2f-firebase-adminsdk-fbsvc-a00d605692.json",
        "firebase-credentials.json",
    };

    @Value("${notification.firebase.credentials-path:}")
    private String credentialsPath;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            InputStream stream = credentialsStream();
            if (stream == null) {
                throw new IllegalStateException(
                    "Firebase credentials not found. Set GOOGLE_APPLICATION_CREDENTIALS, notification.firebase.credentials-path, "
                        + "or place your service account JSON under <repo>/firebase-credentials/ (see firebase-credentials/README.md). "
                        + "user.dir=" + System.getProperty("user.dir"));
            }
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(stream))
                .build();
            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }

    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        return FirestoreClient.getFirestore(firebaseApp);
    }

    private InputStream credentialsStream() throws IOException {
        if (StringUtils.hasText(credentialsPath)) {
            File f = new File(credentialsPath.trim());
            if (f.isFile()) return new FileInputStream(f);
        }
        String envPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (StringUtils.hasText(envPath)) {
            File f = new File(envPath.trim());
            if (f.isFile()) return new FileInputStream(f);
        }
        File credDir = findProjectFirebaseCredentialsDir();
        if (credDir == null) {
            return null;
        }
        for (String name : KNOWN_CREDENTIAL_FILENAMES) {
            File f = new File(credDir, name);
            if (f.isFile()) return new FileInputStream(f);
        }
        File[] adminsdk = credDir.listFiles(
            (d, n) -> n != null && n.endsWith(".json") && n.contains("firebase-adminsdk"));
        if (adminsdk != null && adminsdk.length > 0) {
            return new FileInputStream(adminsdk[0]);
        }
        return null;
    }

    /**
     * Walks up from {@code user.dir} (IDE / Maven working directory varies) until a
     * {@code firebase-credentials} folder containing at least one {@code .json} file is found.
     */
    private static File findProjectFirebaseCredentialsDir() throws IOException {
        File dir = new File(System.getProperty("user.dir")).getCanonicalFile();
        for (int depth = 0; depth < 12 && dir != null; depth++) {
            File candidate = new File(dir, "firebase-credentials");
            if (candidate.isDirectory() && directoryHasJsonFile(candidate)) {
                return candidate;
            }
            dir = dir.getParentFile();
        }
        return null;
    }

    private static boolean directoryHasJsonFile(File dir) {
        File[] files = dir.listFiles((d, n) -> n != null && n.endsWith(".json"));
        return files != null && files.length > 0;
    }
}
