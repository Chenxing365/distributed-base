// MongoDB 初始化脚本
// 容器首次启动时自动执行，创建应用专用数据库与用户

db = db.getSiblingDB('distributed_base');

// 创建应用专用用户（权限最小化）
db.createUser({
  user: 'app_user',
  pwd:  'app_password_change_me',
  roles: [{ role: 'readWrite', db: 'distributed_base' }]
});

// 初始化 system_logs 集合与索引
db.createCollection('system_logs');
db.system_logs.createIndex({ module: 1, action: 1 }, { name: 'idx_module_action' });
db.system_logs.createIndex({ operator: 1, created_at: -1 }, { name: 'idx_operator_created' });
db.system_logs.createIndex({ trace_id: 1 }, { unique: true, sparse: true, name: 'idx_trace_id' });

// 插入初始化测试文档（验证集合可用）
db.system_logs.insertOne({
  trace_id:   'init-trace-000',
  module:     'SYSTEM',
  action:     'INIT',
  operator:   'docker-init',
  content:    'MongoDB 初始化成功，distributed_base 数据库就绪',
  result:     'SUCCESS',
  elapsed_ms: 0,
  client_ip:  '127.0.0.1',
  created_at: new Date(),
  updated_at: new Date()
});

print('✅ distributed_base 数据库初始化完成');
