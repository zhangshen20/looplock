from hashlib import sha256


def target_hash(package_name: str) -> str:
    """Match Android's lowercase SHA-256 package hash without retaining the input."""
    return sha256(package_name.encode("utf-8")).hexdigest()
