"""
批量插入用户信息【可配置分块+多线程版】
✅ 可灵活修改：线程数，起始id，预览
✅ 可自定义插入字段开关，除用户名外
✅ avatar_url = https://i.pravatar.cc/150?u={id}，国外头像占位符
"""

import requests
import random
import string
from concurrent.futures import ThreadPoolExecutor, as_completed
import threading

BASE_URL = "http://localhost/api/users"
COUNT_API_URL = "http://localhost/api/users/count"
NAMES_FILE = "名字.txt"

# ========== 【配置项 - 自定义字段开关 & 预览模式】 ==========
START_ID = 530034          # 填0自动调用接口获取
MAX_WORKERS = 100          # 并发线程数，建议8~20，不要设置过大
PREVIEW_MODE = False       # True=预览(只打印前10条，不提交接口)  False=正式入库
PREVIEW_LIMIT = 10        # 预览模式展示前N条
# 👉 字段开关：True=携带该字段提交，False=不传
FIELD_CONFIG = {
    "id": True,
    "username": True,
    "email": True,
    "password": True,
    "avatar_url": True,
    "gender": True,
    "nickname": True,
    "age": True          # 新增age，范围18~88
}
# ==============================================================

# 线程锁，多线程共享变量必须加锁
id_lock = threading.Lock()
stat_lock = threading.Lock()

next_id = 0
success_count = 0
failed_list = []


def get_start_id_from_api():
    """调用count接口获取起始id: count+1"""
    try:
        resp = requests.get(COUNT_API_URL, timeout=10)
        json_data = resp.json()
        if json_data.get("success"):
            count = json_data.get("count", 0)
            next_start_id = count + 1
            print(f"📊 接口获取用户总数:{count}, 起始ID={next_start_id}")
            return next_start_id
        else:
            raise Exception(f"接口返回失败 {json_data}")
    except Exception as e:
        print(f"❌ 获取数量接口异常: {e}")
        raise e


def read_names(filepath):
    with open(filepath, "r", encoding="utf-8-sig") as f:
        content = f.read().strip()
    content = content.replace("、", " ")
    names = [name.strip() for name in content.split() if name.strip()]
    return names


def random_password(length=12):
    chars = string.ascii_letters + string.digits
    return ''.join(random.choice(chars) for _ in range(length))


def random_email():
    suffix = ''.join(random.choices(string.ascii_lowercase + string.digits, k=random.randint(4, 12)))
    return f"{suffix}@163.com"


def random_gender():
    return random.choice(["男", "女"])


def random_age():
    """age 取值 18 ~ 88 随机整数"""
    return random.randint(18, 88)


def build_user_dict(name, current_id):
    """组装用户json字典，抽离出来给预览/正式共用"""
    user = {}
    if FIELD_CONFIG["id"]:
        user["id"] = current_id
    if FIELD_CONFIG["username"]:
        user["username"] = name
    if FIELD_CONFIG["email"]:
        user["email"] = random_email()
    if FIELD_CONFIG["password"]:
        user["password"] = random_password(12)
    if FIELD_CONFIG["avatar_url"]:
        user["avatar_url"] = f"https://i.pravatar.cc/150?u={current_id}"
    if FIELD_CONFIG["gender"]:
        user["gender"] = random_gender()
    if FIELD_CONFIG["nickname"]:
        user["nickname"] = f"用户{current_id}"
    if FIELD_CONFIG["age"]:
        user["age"] = random_age()
    return user


def create_one_user(name):
    """单个用户创建任务，给线程执行"""
    global next_id, success_count, failed_list

    # 🔐 锁保护ID自增，保证每个线程拿到唯一id
    with id_lock:
        current_id = next_id
        next_id += 1

    user = build_user_dict(name, current_id)

    try:
        resp = requests.post(BASE_URL, json=user, timeout=10)
        data = resp.json()
        if data.get("success"):
            with stat_lock:
                success_count += 1
            return (True, current_id, name, f"password={user.get('password','')}", user.get("avatar_url"))
        else:
            msg = data.get("message", "未知错误")
            return (False, current_id, name, None, msg)
    except Exception as e:
        return (False, current_id, name, None, str(e))


def preview_data(names, start_id):
    """预览模式：只生成打印前PREVIEW_LIMIT条，不请求接口"""
    print(f"\n🔍【预览模式开启，仅生成前 {PREVIEW_LIMIT} 条样例，不会调用接口、不会写入数据库】")
    print(f"📋 当前启用字段: {[k for k,v in FIELD_CONFIG.items() if v]}\n")
    preview_names = names[:PREVIEW_LIMIT]
    current_id = start_id
    for idx, name in enumerate(preview_names, 1):
        user = build_user_dict(name, current_id)
        print(f"--- 预览第{idx}条 | id={current_id} ---")
        print(user)
        current_id += 1
    print(f"\n✅ 预览完成！如需正式入库，请修改 PREVIEW_MODE = False\n")


def batch_insert_concurrent(names, start_id):
    global next_id
    next_id = start_id
    total = len(names)
    print(f"🚀 开启并发导入，线程数:{MAX_WORKERS}, 总任务:{total}")
    print(f"📋 当前启用字段: {[k for k,v in FIELD_CONFIG.items() if v]}")

    with ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
        futures = [executor.submit(create_one_user, name) for name in names]

        for idx, future in enumerate(as_completed(futures), 1):
            ok, uid, name, password, avatar = future.result()
            if ok:
                print(f"[{idx}/{total}] ✅ id={uid} {name} 成功 {password} avatar={avatar}")
            else:
                with stat_lock:
                    failed_list.append((uid, name, password, avatar))
                print(f"[{idx}/{total}] ❌ id={uid} {name} 失败: {password}")

    print(f"\n===== 导入完成 =====")
    print(f"成功: {success_count}/{total}, 失败: {len(failed_list)}")
    if failed_list:
        print("失败列表：")
        for uid, name, pwd, avatar in failed_list:
            print(f"  - id={uid} {name} | pwd={pwd} | avatar={avatar}")


if __name__ == "__main__":
    names = read_names(NAMES_FILE)
    print(f"读取到 {len(names)} 个名字")

    if START_ID <= 0:
        start_id = get_start_id_from_api()
    else:
        start_id = START_ID
        print(f"✍️ 使用手动指定起始ID = {start_id}")

    if PREVIEW_MODE:
        preview_data(names, start_id)
    else:
        batch_insert_concurrent(names, start_id)