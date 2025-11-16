from django.core.cache import cache
from multiprocessing import shared_memory
import pickle


class Store:
    __key = None
    __data = dict()

    def set_data(self, key: str, value):
        self.__data[key] = value

        blob = pickle.dumps(value, protocol=pickle.HIGHEST_PROTOCOL)

        # reset memory
        shm_name = cache.get(f"{key}_shm_name")
        if shm_name is not None: shared_memory.SharedMemory(shm_name).close()

        shm = shared_memory.SharedMemory(create=True, size=len(blob))
        shm.buf[:len(blob)] = blob

        cache.set(f"{key}_shm_name", shm.name, timeout=None)
        cache.set(f"{key}_shm_len", len(blob), timeout=None)

    def get_data(self, key: str):
        if self.__data[key] is not None:
            return self.__data[key]

        shm_name = cache.get(f"{key}_shm_name")
        shm_len = cache.get(f"{key}_shm_len")

        shm = shared_memory.SharedMemory(name=shm_name)
        blob = bytes(shm.buf[:shm_len])

        self.__data[key] = pickle.loads(blob)
        return self.__data[key]

store = Store()