# Clover sandbox — buat test sale (untuk E2E M1)

Merchant BP: **Bp Audit shield** · ID `26V6BKSEAX311`

## 1. Login sandbox (bukan production)

Buka: **https://sandbox.dev.clover.com**  
Login dengan akun developer Bos (email di dashboard).

## 2. Pilih merchant yang benar

Di dropdown merchant, pilih **Bp Audit shield** — **bukan** "Test Merchant" lain.

Cek ID di **Setup → Merchants** → kolom ID = `26V6BKSEAX311`.

## 3. Buat sale di Register

1. Menu kiri → **Register** (atau **Sales** / **New sale**)
2. Tambah item manual — contoh **$15.00**
3. Tap **Charge** / **Pay**
4. Pilih **Cash**
5. Tap **Done** / **Complete** — tunggu konfirmasi hijau

Sale harus **selesai** (bukan hanya open order / cart).

## 4. Verifikasi via API (opsional)

```powershell
python scripts/test_clover_api.py
npm run m1:live
```

Harusnya payments > 0.

## 5. Jalankan E2E

```powershell
npm run e2e:sandbox
```

Atau langsung jika payment ID sudah diketahui:

```powershell
npx tsx scripts/e2e_sandbox_pipeline.ts --payment-id=PAYMENT_ID_HERE
```

## Troubleshooting

| Gejala | Penyebab |
|--------|----------|
| Polling 180s, 0 payment | Sale belum complete, atau merchant salah |
| Sale ada di UI tapi API 0 | Biasanya merchant ID berbeda dari `.env` |
| Token 401 | Jalankan `python scripts/sync_env_from_downloads.py` |

Token sandbox **read-only** untuk create order via API — sale **harus** lewat Register UI.
