import { describe, expect, it } from 'vitest';
import { versionCodeFromName } from './version-code';

describe('versionCodeFromName', () => {
  it('X.Y.Z를 X*10000+Y*100+Z로 바꾼다', () => {
    expect(versionCodeFromName('0.3.3')).toBe(303);
    expect(versionCodeFromName('0.4.0')).toBe(400);
    expect(versionCodeFromName('1.0.0')).toBe(10000);
    expect(versionCodeFromName('12.34.56')).toBe(123456);
  });
  it('꼴이 틀리면 null', () => {
    expect(versionCodeFromName('0.3')).toBeNull();
    expect(versionCodeFromName('v0.3.3')).toBeNull();
    expect(versionCodeFromName('0.3.3-rc1')).toBeNull();
    expect(versionCodeFromName('0.300.3')).toBeNull();
    expect(versionCodeFromName('')).toBeNull();
  });
  it('버전이 오르면 코드도 오른다', () => {
    expect(versionCodeFromName('0.3.9')! < versionCodeFromName('0.4.0')!).toBe(true);
    expect(versionCodeFromName('0.99.99')! < versionCodeFromName('1.0.0')!).toBe(true);
  });
});
