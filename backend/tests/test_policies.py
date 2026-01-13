from datetime import date, timedelta

from app.domain.policies import determine_status


def test_determine_status_fresh_no_expiry():
    assert determine_status(None, date.today(), 3) == "fresh"


def test_determine_status_expired():
    today = date.today()
    assert determine_status(today - timedelta(days=1), today, 3) == "expired"


def test_determine_status_expiring_threshold():
    today = date.today()
    assert determine_status(today + timedelta(days=2), today, 3) == "expiring"


def test_determine_status_fresh_above_threshold():
    today = date.today()
    assert determine_status(today + timedelta(days=5), today, 3) == "fresh"
