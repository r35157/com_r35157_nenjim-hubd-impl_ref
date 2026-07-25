python3 -c '
import json
import sys

source, destination = sys.argv[1], sys.argv[2]

with open(source) as f:
    key = json.load(f)

if len(key) not in (32, 64):
    raise SystemExit(f"Unexpected key length: {len(key)}")

if not all(isinstance(value, int) and 0 <= value <= 255 for value in key):
    raise SystemExit("The file contains invalid byte values")

with open(destination, "w") as f:
    f.write(json.dumps(key, separators=(",", ":")))

print(f"Created {destination} with {len(key)} bytes")
' $1 $2