const EMAILJS_API_URL = 'https://api.emailjs.com/api/v1.0/email/send';

function isMailConfigured() {
  return !!(
    process.env.EMAILJS_SERVICE_ID &&
    process.env.EMAILJS_TEMPLATE_ID &&
    process.env.EMAILJS_PUBLIC_KEY
  );
}

function formatCurrency(amount) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);
}

async function sendBookingConfirmationEmail({ to, booking }) {
  if (!to) {
    console.log('[mail] Bỏ qua gửi vé: tài khoản chưa thiết lập Gmail trong hồ sơ.');
    return;
  }
  if (!isMailConfigured()) {
    console.warn('[mail] Chưa cấu hình EMAILJS_SERVICE_ID/EMAILJS_TEMPLATE_ID/EMAILJS_PUBLIC_KEY, bỏ qua gửi email vé.');
    return;
  }

  const combos = booking.combos || [];
  const combosText = combos.length > 0
    ? combos.map((c) => `${c.name} x${c.quantity}`).join(', ')
    : 'Không có';

  const response = await fetch(EMAILJS_API_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      service_id: process.env.EMAILJS_SERVICE_ID,
      template_id: process.env.EMAILJS_TEMPLATE_ID,
      user_id: process.env.EMAILJS_PUBLIC_KEY,
      accessToken: process.env.EMAILJS_PRIVATE_KEY || undefined,
      template_params: {
        to_email: to,
        film_title: booking.filmTitle,
        show_date: booking.date,
        show_time: booking.time,
        seats: (booking.seats || []).join(', '),
        combos: combosText,
        total_price: formatCurrency(booking.totalPrice),
        booking_id: booking.id
      }
    })
  });

  if (!response.ok) {
    const errorBody = await response.text().catch(() => '');
    throw new Error(`EmailJS API trả về lỗi ${response.status}: ${errorBody}`);
  }

  console.log(`[mail] Đã gửi email vé tới ${to} (booking ${booking.id})`);
}

module.exports = { sendBookingConfirmationEmail, isMailConfigured };