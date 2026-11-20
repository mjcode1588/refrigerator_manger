from collections import defaultdict
from dataclasses import dataclass

from app.db.models import StorageLocation


@dataclass
class LocationNodeInfo:
    location: StorageLocation
    depth: int
    full_path: str
    children: list["LocationNodeInfo"]


def build_location_paths(locations: list[StorageLocation]) -> dict[int, tuple[int, str]]:
    """각 location_id에 대해 (depth, '부모 > 자식' 전체 경로)를 계산합니다."""
    by_id = {loc.id: loc for loc in locations}
    memo: dict[int, tuple[int, str]] = {}

    def resolve(loc_id: int, visiting: set[int]) -> tuple[int, str]:
        if loc_id in memo:
            return memo[loc_id]
        loc = by_id[loc_id]
        if loc.parent_id is None or loc.parent_id not in by_id or loc.parent_id in visiting:
            res = (0, loc.name)
        else:
            p_depth, p_path = resolve(loc.parent_id, visiting | {loc_id})
            res = (p_depth + 1, f"{p_path} > {loc.name}")
        memo[loc_id] = res
        return res

    for loc in locations:
        resolve(loc.id, set())
    return memo


def build_location_tree(locations: list[StorageLocation]) -> list[LocationNodeInfo]:
    paths = build_location_paths(locations)
    children_map: dict[int | None, list[StorageLocation]] = defaultdict(list)
    for loc in sorted(locations, key=lambda x: (x.sort_order, x.id)):
        children_map[loc.parent_id].append(loc)

    def make_node(loc: StorageLocation) -> LocationNodeInfo:
        depth, full_path = paths[loc.id]
        return LocationNodeInfo(
            location=loc,
            depth=depth,
            full_path=full_path,
            children=[make_node(child) for child in children_map.get(loc.id, [])],
        )

    return [make_node(root) for root in children_map.get(None, [])]


def collect_descendant_ids(locations: list[StorageLocation], root_id: int) -> set[int]:
    """root_id 자신을 포함한 모든 하위 보관 장소 ID 집합을 반환합니다."""
    children_map: dict[int | None, list[int]] = defaultdict(list)
    for loc in locations:
        children_map[loc.parent_id].append(loc.id)

    result: set[int] = set()
    stack = [root_id]
    while stack:
        current = stack.pop()
        if current in result:
            continue
        result.add(current)
        stack.extend(children_map.get(current, []))
    return result


def would_create_cycle(
    locations: list[StorageLocation], target_id: int, new_parent_id: int | None
) -> bool:
    """target_id의 부모를 new_parent_id로 변경할 때 순환 참조가 발생하는지 검사합니다."""
    if new_parent_id is None:
        return False
    if new_parent_id == target_id:
        return True
    descendants = collect_descendant_ids(locations, target_id)
    return new_parent_id in descendants
