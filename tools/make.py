#!/usr/bin/env python3

import sqlite3, os, sys
from collections import defaultdict
from time import time
from tables import *

start = time()

dicts = defaultdict(dict)
if len(sys.argv) > 1:
	argv = sys.argv[1:]
	langs = getLangs(dicts, argv)
else:
	langs = getLangs(dicts)
keys = [f"{lang}" for lang in langs]
fields = [f"`{i}`" for i in keys]
CREATE = 'CREATE VIRTUAL TABLE mcpdict USING fts3 (%s)' % (",".join(fields))
INSERT = 'INSERT INTO mcpdict VALUES (%s)'% (','.join('?' * len(keys)))

#db
NAME = '../app/src/main/assets/databases/mcpdict.db'
DIR = os.path.dirname(NAME)
if os.path.exists(NAME): os.remove(NAME)
if not os.path.exists(DIR): os.mkdir(DIR)
conn = sqlite3.connect(NAME)
c = conn.cursor()
for i in keys:
	if keys.count(i) > 1:
		print(f"{i}重名")
		exit()
c.execute(CREATE)
for i in sorted(dicts.keys(), key=cjkorder):
	v = list(map(dicts[i].get, keys))
	c.execute(INSERT, v)

#info
keys = list(langs[辭典數 if len(keys) > 辭典數 else 1].info.keys())
keys.remove("文件格式")
keys.remove("跳過行數")
CREATE = 'CREATE VIRTUAL TABLE info USING fts3 (%s)' % (",".join(keys))
INSERT = 'INSERT INTO info VALUES (%s)'% (','.join('?' * len(keys)))
c.execute(CREATE)
for lang in langs:
	v = list(map(lang.info.get, keys))
	c.execute(INSERT, v)

conn.commit()
conn.close()

passed = time() - start
print(f"({len(dicts):5d}) {passed:6.3f} 保存")
