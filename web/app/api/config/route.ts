import { getRepo } from '../../../lib/repo';

export const dynamic = 'force-dynamic';

const num = (v: string | undefined) => {
  const n = Number(v);
  return Number.isNaN(n) ? 0 : n;
};

export async function GET(): Promise<Response> {
  const s = await getRepo().settings.all();
  return Response.json({
    prices: { day: num(s.price_day), lifetime: num(s.price_lifetime), month: num(s.price_month), year: num(s.price_year) },
    bankAccount: s.bank_account ?? '',
    openchatUrl: s.openchat_url ?? '',
    termsUrl: '/terms',
  });
}
