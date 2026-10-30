import { describe, expect, it } from 'vitest';
import { createMemoryRepo } from '../../../lib/repo.memory';
import { saveSettingsCore } from './settings-admin';

describe('saveSettingsCore', () => {
  it('유효한 값 저장(트림)', async () => {
    const repo = createMemoryRepo();
    await saveSettingsCore(repo, { price_month: ' 5000 ', bank_account: ' 국민 123 ', openchat_url: 'https://open.kakao.com/x', notice: '' });
    const s = await repo.settings.all();
    expect(s.price_month).toBe('5000');
    expect(s.bank_account).toBe('국민 123');
    expect(s.openchat_url).toBe('https://open.kakao.com/x');
  });
  it('잘못된 가격은 throw, 아무것도 저장 안 함', async () => {
    const repo = createMemoryRepo();
    const before = await repo.settings.all();
    await expect(saveSettingsCore(repo, { bank_account: 'x', price_year: '-1' })).rejects.toThrow('invalid price');
    await expect(saveSettingsCore(repo, { price_year: '1.5' })).rejects.toThrow('invalid price');
    await expect(saveSettingsCore(repo, { price_year: '' })).rejects.toThrow('invalid price');
    expect(await repo.settings.all()).toEqual(before);
  });
  it('openchat_url은 https 또는 빈 값', async () => {
    const repo = createMemoryRepo();
    await expect(saveSettingsCore(repo, { openchat_url: 'http://x' })).rejects.toThrow('invalid url');
    await saveSettingsCore(repo, { openchat_url: '' });
  });
  it('모르는 키는 무시', async () => {
    const repo = createMemoryRepo();
    await saveSettingsCore(repo, { evil: '1' });
    expect((await repo.settings.all()).evil).toBeUndefined();
  });
});
