# ========================================
# START OF CODE
# ========================================

"""In-memory Firestore stand-in so route tests do not need a live project."""

from __future__ import annotations


class FakeSnapshot:
    def __init__(self, doc_id: str, data: dict | None):
        self.id = doc_id
        self._data = data
        self.exists = data is not None

    def to_dict(self) -> dict | None:
        return dict(self._data) if self._data is not None else None


class FakeDocument:
    def __init__(self, store: dict[str, dict], collection: str, doc_id: str):
        self._store = store
        self._collection = collection
        self.id = doc_id

    def get(self) -> FakeSnapshot:
        return FakeSnapshot(self.id, self._store.get(self._collection, {}).get(self.id))

    def set(self, payload: dict, merge: bool = False) -> None:
        bucket = self._store.setdefault(self._collection, {})
        if merge and self.id in bucket:
            bucket[self.id] = {**bucket[self.id], **payload}
        else:
            bucket[self.id] = dict(payload)

    def update(self, payload: dict) -> None:
        self._store.setdefault(self._collection, {}).setdefault(self.id, {}).update(payload)

    def delete(self) -> None:
        self._store.get(self._collection, {}).pop(self.id, None)

    @property
    def reference(self) -> "FakeDocument":
        return self


class FakeCollection:
    def __init__(self, store: dict[str, dict], collection: str):
        self._store = store
        self._collection = collection
        self._next_id = 1

    def document(self, doc_id: str | None = None) -> FakeDocument:
        if not doc_id:
            doc_id = f"auto-{self._next_id}"
            self._next_id += 1
        return FakeDocument(self._store, self._collection, doc_id)

    def stream(self):
        for doc_id, data in list(self._store.get(self._collection, {}).items()):
            yield FakeSnapshot(doc_id, data)


class FakeDb:
    def __init__(self):
        self.store: dict[str, dict] = {}

    def collection(self, name: str) -> FakeCollection:
        return FakeCollection(self.store, name)

# ========================================
# END OF CODE
# ========================================
