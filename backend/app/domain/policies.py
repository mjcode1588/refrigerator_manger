from datetime import date


def determine_status(expiry_date: date | None, today: date, expiring_days: int) -> str:
    if not expiry_date:
        return "fresh"
    if expiry_date < today:
        return "expired"
    if (expiry_date - today).days <= expiring_days:
        return "expiring"
    return "fresh"
