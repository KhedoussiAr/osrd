import json

import requests

from tests.get_rolling_stocks import get_rolling_stock
from tests.get_schedule import get_schedule
from tests.run_pathfinding import run_pathfinding


def run_simulation(base_url, infra_id):
    path_id = run_pathfinding(base_url, infra_id)
    rolling_stock_id = get_rolling_stock(base_url)
    schedule_payload = make_payload_schedule(base_url, infra_id, path_id, rolling_stock_id)
    r = requests.post(base_url + "train_schedule/", json=schedule_payload)
    if r.status_code // 100 != 2:
        raise RuntimeError(f"Schedule error {r.status_code}: {r.content}, payload={json.dumps(schedule_payload)}")

    schedule_id = r.json()["id"]
    return schedule_id


def make_payload_schedule(base_url, infra, path, rolling_stock):
    return {
        "train_name": "foo",
        "labels": [],
        "departure_time": 0,
        "phases": [],
        "margins": [],
        "initial_speed": 0,
        "timetable": get_schedule(base_url, infra),
        "rolling_stock": rolling_stock,
        "path": path
    }


def run(*args, **kwargs):
    base_url = kwargs["url"]
    infra_id = kwargs["infra_id"]
    run_simulation(base_url, infra_id)
    return True, ""