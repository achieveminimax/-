import csv, os

modules = ['seckill-user', 'seckill-goods', 'seckill-order', 'seckill-seckill', 'seckill-admin']
service_keywords = ['service', 'mq', 'interceptor', 'task', 'aspect']

print(f"{'Class':<50} {'Instr%':>7} {'Line%':>7} {'Branch%':>8}")
print("-" * 75)

for mod in modules:
    csv_path = f"{mod}/target/site/jacoco/jacoco.csv"
    if not os.path.exists(csv_path):
        continue
    with open(csv_path) as f:
        reader = csv.DictReader(f)
        for row in reader:
            pkg = row["PACKAGE"]
            cls = row["CLASS"]
            if any(sk in pkg for sk in service_keywords):
                im = int(row["INSTRUCTION_MISSED"])
                ic = int(row["INSTRUCTION_COVERED"])
                lm = int(row["LINE_MISSED"])
                lc = int(row["LINE_COVERED"])
                bm = int(row["BRANCH_MISSED"])
                bc = int(row["BRANCH_COVERED"])

                i_pct = ic / (im + ic) * 100 if (im + ic) > 0 else 0
                l_pct = lc / (lm + lc) * 100 if (lm + lc) > 0 else 0
                b_pct = bc / (bm + bc) * 100 if (bm + bc) > 0 else 0

                flag = " ❌" if i_pct < 80 else " ✅"
                print(f"{cls:<50} {i_pct:>6.1f}%{flag} {l_pct:>6.1f}% {b_pct:>7.1f}%")
