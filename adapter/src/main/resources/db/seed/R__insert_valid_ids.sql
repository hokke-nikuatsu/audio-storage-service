-- 有効なユーザー ID とフレーズ ID の初期データ。繰り返し実行のマイグレーションなので、このファイルを変えると ID の入った DB にもう一度流れる。すでにある ID は飛ばす
INSERT OR IGNORE INTO users (id) VALUES (1), (2), (3);
INSERT OR IGNORE INTO phrases (id) VALUES (1), (2), (3);
