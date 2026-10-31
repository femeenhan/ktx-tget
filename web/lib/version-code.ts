/**
 * versionName(X.Y.Z)에서 versionCode를 만든다. 앱의 app/build.gradle.kts `versionCodeOf`와 같은 공식이다.
 * 각 자리는 0~99. 꼴이 맞지 않으면 null.
 */
export function versionCodeFromName(versionName: string): number | null {
  const m = /^(\d{1,2})\.(\d{1,2})\.(\d{1,2})$/.exec(versionName);
  if (!m) return null;
  const [major, minor, patch] = [Number(m[1]), Number(m[2]), Number(m[3])];
  return major * 10000 + minor * 100 + patch;
}
