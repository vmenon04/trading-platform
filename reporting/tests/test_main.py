"""Tests for scripts/main.py.   Run:  python -m pytest -v   (from the reporting folder)

main.py stops the run (SystemExit) rather than save a misleading report.
"""
import pandas as pd
import pytest

import main


def test_good_report_passes():
    main.check(pd.DataFrame({"notional": [10.0, 0.0]}), "volume")   # no exception = pass


def test_empty_report_stops_the_run():
    with pytest.raises(SystemExit):
        main.check(pd.DataFrame(), "volume")


@pytest.mark.parametrize("column", ["notional", "units", "BUY", "SELL"])
def test_negative_values_stop_the_run(column):
    with pytest.raises(SystemExit):
        main.check(pd.DataFrame({column: [10.0, -1.0]}), "volume")
