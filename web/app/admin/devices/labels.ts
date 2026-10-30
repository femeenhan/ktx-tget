import type { DeviceStatus, Plan } from '../../../lib/license';

export const PLAN_LABEL: Record<Plan, string> = { lifetime: '영구', month: '1개월', year: '1년' };
export const STATUS_LABEL: Record<DeviceStatus, string> = { pending: '대기', active: '활성', expired: '만료', revoked: '회수' };
