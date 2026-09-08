const nodemailer = require('nodemailer');

let transporter = null;

function getTransporter() {
  if (!transporter) {
    transporter = nodemailer.createTransport({
      host: process.env.SMTP_HOST || 'smtp.gmail.com',
      port: parseInt(process.env.SMTP_PORT || '465', 10),
      secure: process.env.SMTP_SECURE ? process.env.SMTP_SECURE === 'true' : true,
      auth: {
        user: process.env.SMTP_USER,
        pass: process.env.SMTP_PASS
      }
    });
  }
  return transporter;
}

function isMailConfigured() {
  return !!(process.env.SMTP_USER && process.env.SMTP_PASS);
}

function formatCurrency(amount) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);
}

function buildTicketHtml(booking) {
  const seatsText = (booking.seats || []).join(', ');
  return `
    <div style="font-family: Arial, Helvetica, sans-serif; max-width: 480px; margin: 0 auto; border: 1px solid #eee; border-radius: 12px; overflow: hidden;">
      <div style="background: linear-gradient(135deg, #e57373, #f06292); padding: 20px; color: #fff;">
        <h2 style="margin: 0;">🎬 Vé xem phim của bạn</h2>
      </div>
      <div style="padding: 20px; color: #333;">
        <p>Cảm ơn bạn đã đặt vé! Dưới đây là thông tin chi tiết:</p>
        <table style="width: 100%; border-collapse: collapse; font-size: 14px;">
          <tr>
            <td style="padding: 6px 0; color: #888; width: 40%;">Phim</td>
            <td style="padding: 6px 0; font-weight: bold;">${booking.filmTitle}</td>
          </tr>
          <tr>
            <td style="padding: 6px 0; color: #888;">Suất chiếu</td>
            <td style="padding: 6px 0;">${booking.date} • ${booking.time}</td>
          </tr>
          <tr>
            <td style="padding: 6px 0; color: #888;">Ghế</td>
            <td style="padding: 6px 0;">${seatsText}</td>
          </tr>
          <tr>
            <td style="padding: 6px 0; color: #888;">Tổng tiền</td>
            <td style="padding: 6px 0; font-weight: bold; color: #e57373;">${formatCurrency(booking.totalPrice)}</td>
          </tr>
          <tr>
            <td style="padding: 6px 0; color: #888;">Mã vé</td>
            <td style="padding: 6px 0;">${booking.id}</td>
          </tr>
        </table>
        <p style="margin-top: 20px; font-size: 12px; color: #999;">
          Vui lòng xuất trình mã vé này tại quầy để nhận vé. Chúc bạn xem phim vui vẻ!
        </p>
      </div>
    </div>
  `;
}

async function sendBookingConfirmationEmail({ to, booking }) {
  if (!to) {
    console.log('[mail] Bỏ qua gửi vé: tài khoản chưa thiết lập Gmail trong hồ sơ.');
    return;
  }
  if (!isMailConfigured()) {
    console.warn('[mail] Chưa cấu hình SMTP_USER/SMTP_PASS, bỏ qua gửi email vé.');
    return;
  }

  const transport = getTransporter();
  await transport.sendMail({
    from: process.env.MAIL_FROM || process.env.SMTP_USER,
    to,
    subject: `Vé xem phim: ${booking.filmTitle}`,
    html: buildTicketHtml(booking)
  });

  console.log(`[mail] Đã gửi email vé tới ${to} (booking ${booking.id})`);
}

module.exports = { sendBookingConfirmationEmail, isMailConfigured };
