package com.esprit.notification.service;

import com.esprit.notification.dto.NotificationRequest;
import com.esprit.notification.dto.NotificationResponse;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "null"})
class NotificationServiceFirestoreTest {

    @Test
    void create_usesFirestoreWhenAvailable() throws Exception {
        Firestore firestore = mock(Firestore.class);
        ObjectProvider<Firestore> provider = mockProvider(firestore);

        CollectionReference collection = mock(CollectionReference.class);
        when(firestore.collection("notifications")).thenReturn(collection);

        ApiFuture<DocumentReference> addFuture = (ApiFuture<DocumentReference>) mock(ApiFuture.class);
        DocumentReference ref = mock(DocumentReference.class);
        when(collection.add(org.mockito.ArgumentMatchers.<Map<String, Object>>argThat(Objects::nonNull))).thenReturn(addFuture);
        when(addFuture.get()).thenReturn(ref);
        when(ref.getId()).thenReturn("doc-1");

        ApiFuture<DocumentSnapshot> docFuture = (ApiFuture<DocumentSnapshot>) mock(ApiFuture.class);
        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
        when(ref.get()).thenReturn(docFuture);
        when(docFuture.get()).thenReturn(snapshot);
        when(snapshot.getData()).thenReturn(Map.of(
            "userId", "u-1",
            "title", "hello",
            "body", "body",
            "type", "INFO",
            "read", false,
            "createdAt", Instant.now().toString()
        ));

        NotificationService service = new NotificationService(provider);
        NotificationResponse response = service.create(NotificationRequest.builder().userId("u-1").title("hello").build());

        assertThat(response.getId()).isEqualTo("doc-1");
        assertThat(response.getUserId()).isEqualTo("u-1");
    }

    @Test
    void findByUserId_usesFirestoreQuery() throws Exception {
        Firestore firestore = mock(Firestore.class);
        ObjectProvider<Firestore> provider = mockProvider(firestore);

        CollectionReference collection = mock(CollectionReference.class);
        Query query = mock(Query.class);
        ApiFuture<QuerySnapshot> queryFuture = (ApiFuture<QuerySnapshot>) mock(ApiFuture.class);
        QuerySnapshot querySnapshot = mock(QuerySnapshot.class);
        QueryDocumentSnapshot doc = mock(QueryDocumentSnapshot.class);

        when(firestore.collection("notifications")).thenReturn(collection);
        when(collection.whereEqualTo("userId", "u-2")).thenReturn(query);
        when(query.get()).thenReturn(queryFuture);
        when(queryFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.getDocuments()).thenReturn(List.of(doc));
        when(doc.getId()).thenReturn("n-1");
        when(doc.getData()).thenReturn(Map.of(
            "userId", "u-2",
            "title", "new",
            "body", "",
            "type", "GENERAL",
            "read", false,
            "createdAt", Instant.now().toString()
        ));

        NotificationService service = new NotificationService(provider);
        List<NotificationResponse> result = service.findByUserId("u-2");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("n-1");
    }

    @Test
    void markReadAndDelete_useFirestoreOperations() throws Exception {
        Firestore firestore = mock(Firestore.class);
        ObjectProvider<Firestore> provider = mockProvider(firestore);

        CollectionReference collection = mock(CollectionReference.class);
        DocumentReference ref = mock(DocumentReference.class);
        ApiFuture<WriteResult> writeFuture = (ApiFuture<WriteResult>) mock(ApiFuture.class);
        ApiFuture<DocumentSnapshot> getFuture = (ApiFuture<DocumentSnapshot>) mock(ApiFuture.class);
        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);

        when(firestore.collection("notifications")).thenReturn(collection);
        when(collection.document("id-1")).thenReturn(ref);
        when(ref.update("read", true)).thenReturn(writeFuture);
        when(writeFuture.get()).thenReturn(mock(WriteResult.class));
        when(ref.get()).thenReturn(getFuture);
        when(getFuture.get()).thenReturn(snapshot);
        when(snapshot.getData()).thenReturn(Map.of(
            "userId", "u-3",
            "title", "title",
            "body", "body",
            "type", "INFO",
            "read", true,
            "createdAt", Instant.now().toString()
        ));
        when(ref.delete()).thenReturn(writeFuture);

        NotificationService service = new NotificationService(provider);
        NotificationResponse marked = service.markRead("id-1");
        service.delete("id-1");

        assertThat(marked.isRead()).isTrue();
    }

    @Test
    void create_wrapsExecutionExceptionAsRuntime() throws Exception {
        Firestore firestore = mock(Firestore.class);
        ObjectProvider<Firestore> provider = mockProvider(firestore);

        CollectionReference collection = mock(CollectionReference.class);
        ApiFuture<DocumentReference> addFuture = (ApiFuture<DocumentReference>) mock(ApiFuture.class);
        when(firestore.collection("notifications")).thenReturn(collection);
        when(collection.add(org.mockito.ArgumentMatchers.<Map<String, Object>>argThat(Objects::nonNull))).thenReturn(addFuture);
        when(addFuture.get()).thenThrow(new ExecutionException("boom", new RuntimeException("boom")));

        NotificationService service = new NotificationService(provider);
        NotificationRequest request = NotificationRequest.builder().userId("u-4").title("x").build();

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to create notification");
    }

    @Test
    void markRead_wrapsInterruptedExceptionAndSetsInterruptedFlag() throws Exception {
        Firestore firestore = mock(Firestore.class);
        ObjectProvider<Firestore> provider = mockProvider(firestore);

        CollectionReference collection = mock(CollectionReference.class);
        DocumentReference ref = mock(DocumentReference.class);
        ApiFuture<WriteResult> writeFuture = (ApiFuture<WriteResult>) mock(ApiFuture.class);

        when(firestore.collection("notifications")).thenReturn(collection);
        when(collection.document("id-err")).thenReturn(ref);
        when(ref.update("read", true)).thenReturn(writeFuture);
        when(writeFuture.get()).thenThrow(new InterruptedException("interrupted"));

        NotificationService service = new NotificationService(provider);

        assertThatThrownBy(() -> service.markRead("id-err"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to mark notification as read");
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        Thread.interrupted();
    }

    @SuppressWarnings("rawtypes")
    private static ObjectProvider<Firestore> mockProvider(Firestore firestore) {
        ObjectProvider provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(firestore);
        return (ObjectProvider<Firestore>) provider;
    }
}
