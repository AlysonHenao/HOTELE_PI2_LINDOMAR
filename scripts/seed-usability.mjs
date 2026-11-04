// Datos ficticios mediante la API. Por defecto solo admite un servidor local.
const base = new URL(process.env.USABILITY_API_URL || 'http://127.0.0.1:8080/api/');
if (!['localhost', '127.0.0.1', '[::1]'].includes(base.hostname)) {
  throw new Error('El seed solo se permite en local. Usa una base de pruebas aislada.');
}
const session = process.env.USABILITY_SESSION || 'S3';
if (!/^[A-Za-z0-9-]{1,12}$/.test(session)) throw new Error('Sesión inválida');
const password = process.env.USABILITY_PASSWORD || 'UsabilidadS3-2026';
const prefix = `US-${session}`;
async function api(path, token, method = 'GET', data) {
  const response = await fetch(new URL(path, base), {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    ...(data ? { body: JSON.stringify(data) } : {}),
  });
  const result = await response.json().catch(() => null);
  if (!response.ok) throw new Error(`${method} ${path}: ${result?.message || response.status}`);
  return result;
}
const login = (email, pass) => api('auth/login', null, 'POST', { email, password: pass });
const admin = await login(process.env.USABILITY_ADMIN_EMAIL || 'admin@lindomar.co',
  process.env.USABILITY_ADMIN_PASSWORD || 'demo123');
const users = await api('users', admin.token);
async function account(suffix, role) {
  const email = `us-${session.toLowerCase()}-${suffix}@example.test`;
  let user = users.find((u) => u.email === email);
  if (!user) {
    user = (await api('auth/register', null, 'POST', {
      name: `${prefix} ${suffix} (ficticio)`, email, password,
    })).user;
    if (role !== 'GUEST') user = await api(`users/${user.id}/role`, admin.token, 'PATCH', { role });
  }
  if (user.role !== role) throw new Error(`El rol de ${email} cambió; usa otro USABILITY_SESSION`);
  return { user, ...(await login(email, password)) };
}
const guest = await account('huesped', 'GUEST');
const employee = await account('empleado', 'EMPLOYEE');
const allRooms = await api('rooms/all', admin.token);
const createdRooms = [];
for (const [suffix, status, capacity, price] of [
  ['401', 'AVAILABLE', 2, 150000], ['402', 'AVAILABLE', 4, 220000],
  ['403', 'MAINTENANCE', 2, 150000], ['404', 'OUT_OF_SERVICE', 2, 150000],
  ['405', 'RESERVED', 2, 180000],
]) {
  const number = `${prefix}-${suffix}`;
  const room = allRooms.find((r) => r.number === number) || await api('rooms', admin.token, 'POST', {
    number, status, capacity, price, floor: 4, type: capacity === 4 ? 'Familiar' : 'Estándar',
    balcony: suffix === '402', petFriendly: suffix === '402', services: 'Wi-Fi · Desayuno (prueba)',
  });
  createdRooms.push(room);
}
const date = (offset) => {
  // La misma zona utilizada por las reglas del hotel, independientemente del equipo facilitador.
  const parts = new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Bogota',
    year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date());
  const d = new Date(`${parts}T12:00:00Z`);
  d.setUTCDate(d.getUTCDate() + offset);
  return d.toISOString().slice(0, 10);
};
const notes = `${prefix}: reserva ficticia de usabilidad`;
const bookings = await api('reservations/mine', guest.token);
const reservation = bookings.find((r) => r.notes === notes) || await api('reservations', guest.token, 'POST', {
  roomId: createdRooms[0].id, checkIn: date(7), checkOut: date(9), guests: 2, notes,
});
if (reservation.status !== 'CONFIRMED') throw new Error('La reserva ya se usó; prepara otra sesión');
const taskTitle = `${prefix}: preparar habitación 402`;
const task = (await api('tasks', admin.token)).find((t) => t.title === taskTitle) ||
  await api('tasks', admin.token, 'POST', { employeeId: employee.user.id, title: taskTitle,
    description: 'Reponer dos toallas y verificar minibar. Datos ficticios.', dueDate: date(1), priority: 'Alta' });
if (task.status !== 'PENDING') throw new Error('La tarea ya se usó; prepara otra sesión');
const concept = `${prefix}: servicio de lavandería (ficticio)`;
const reminder = (await api('reminders', admin.token)).find((r) => r.concept === concept) ||
  await api('reminders', admin.token, 'POST', { concept, category: 'Servicios', estimatedAmount: 25000,
    dueDate: date(2) });
if (reminder.status !== 'PENDING') throw new Error('El recordatorio ya se usó; prepara otra sesión');
console.log(JSON.stringify({ session, createdAt: new Date().toISOString(),
  guest: guest.user.email, employee: employee.user.email,
  rooms: createdRooms.map(({ id, number, status }) => ({ id, number, status })),
  reservation: { id: reservation.id, checkIn: reservation.checkIn, checkOut: reservation.checkOut },
  taskId: task.id, reminderId: reminder.id,
  note: 'No contiene resultados de participantes. Para otra sesión cambia USABILITY_SESSION.' }, null, 2));
