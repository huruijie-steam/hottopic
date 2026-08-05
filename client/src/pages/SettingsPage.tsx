import { useEffect, useState } from 'react';
import { Save } from 'lucide-react';
import { settingsApi } from '../services/api';

export default function SettingsPage() {
  const [editing, setEditing] = useState<Record<string, string>>({});
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    settingsApi
      .getAll()
      .then(setEditing)
      .catch((e) => setError((e as Error).message));
  }, []);

  const save = async () => {
    try {
      await settingsApi.update(editing);
      setMessage('设置已保存');
      setTimeout(() => setMessage(''), 2000);
    } catch (e) {
      setError((e as Error).message);
    }
  };

  const fields = [
    { key: 'monitor.cron', label: '巡检 cron 表达式', hint: '默认每 30 分钟:0 */30 * * * *' },
    { key: 'mail.enabled', label: '邮件通知开关', hint: 'true / false' },
    { key: 'mail.to', label: '邮件收件人', hint: '多个用逗号分隔' },
  ];

  return (
    <div className="max-w-2xl space-y-4">
      <div className="rounded-xl border border-white/10 bg-white/5 p-4">
        <h2 className="mb-1 font-medium text-gray-100">系统设置</h2>
        <p className="mb-4 text-sm text-gray-500">
          AI Key 与 SMTP 密码通过环境变量配置(不在此处显示);修改后需重启后端生效。
        </p>
        {fields.map((f) => (
          <div key={f.key} className="mb-4">
            <label className="mb-1 block text-sm text-gray-300">{f.label}</label>
            <input
              className="w-full rounded-lg border border-white/10 bg-white/5 px-3 py-2 text-sm text-gray-200 outline-none focus:border-indigo-400"
              value={editing[f.key] ?? ''}
              onChange={(e) => setEditing((prev) => ({ ...prev, [f.key]: e.target.value }))}
            />
            {f.hint && <div className="mt-1 text-xs text-gray-500">{f.hint}</div>}
          </div>
        ))}

        <div className="flex items-center gap-3">
          <button
            className="flex items-center gap-1 rounded-lg bg-indigo-500 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-400"
            onClick={save}
          >
            <Save className="h-4 w-4" /> 保存
          </button>
          {message && <span className="text-sm text-emerald-400">{message}</span>}
          {error && <span className="text-sm text-red-300">{error}</span>}
        </div>
      </div>

      <div className="rounded-xl border border-white/10 bg-white/5 p-4 text-sm text-gray-400">
        <h3 className="mb-2 font-medium text-gray-200">环境变量配置</h3>
        <pre className="overflow-x-auto rounded-lg bg-black/30 p-3 text-xs text-gray-300">{`DEEPSEEK_API_KEY=sk-...   # AI 分析(必填)
MAIL_HOST=smtp.qq.com    # 邮件 SMTP(可选)
MAIL_USERNAME=xxx        # SMTP 账号
MAIL_PASSWORD=xxx        # SMTP 授权码
MAIL_TO=xxx@qq.com       # 收件人
MAIL_ENABLED=true        # 开启邮件通知`}</pre>
      </div>
    </div>
  );
}
