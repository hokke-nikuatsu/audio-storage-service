-- 有効なユーザー ID
CREATE TABLE users (
    id INTEGER NOT NULL PRIMARY KEY
);

-- 有効なフレーズ ID
CREATE TABLE phrases (
    id INTEGER NOT NULL PRIMARY KEY
);

-- ユーザー ID とフレーズ ID の組と、保存した録音のファイルの、保存先のルートからの相対パスの対応。組ごとに1行だけ持つ
CREATE TABLE recordings (
    user_id INTEGER NOT NULL REFERENCES users (id),
    phrase_id INTEGER NOT NULL REFERENCES phrases (id),
    file_path TEXT NOT NULL UNIQUE,
    PRIMARY KEY (user_id, phrase_id)
);
