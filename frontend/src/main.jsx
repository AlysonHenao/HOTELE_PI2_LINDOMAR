import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {createRoot} from 'react-dom/client';
import {
  BedDouble, Bell, BellRing, CalendarClock, CalendarDays, CheckCircle2, CircleDollarSign,
  Camera, ClipboardCheck, Clock3, ConciergeBell, DoorOpen, Eye, LayoutDashboard, LogOut, Map, Menu,
  MessageCircle, Moon, Plus, Search, Settings2, ShieldCheck, Sparkles, Star, Sun, Trash2,
  TrendingDown, TrendingUp, UserPlus, Users, WalletCards, Waves, Wifi, X
} from 'lucide-react';
import './styles.css';

/* ---------- Utilidades ---------- */
const api = async (path, options = {}) => {
  const isFormData = options.body instanceof FormData;
  const token = localStorage.getItem('lindomar-token');
  const headers = {
    ...(!isFormData ? {'Content-Type': 'application/json'} : {}),
    ...(token ? {Authorization: `Bearer ${token}`} : {}),
    ...(options.headers || {})
  };
  const res = await fetch(`/api${path}`, {...options, headers});
  const body = await res.json().catch(() => null);
  if (!res.ok) throw new Error(body?.message || 'No fue posible completar la acción');
  return body;
};
const money = v => new Intl.NumberFormat('es-CO', {style: 'currency', currency: 'COP', maximumFractionDigits: 0}).format(v || 0);
const label = {
  GUEST: 'Huésped', EMPLOYEE: 'Empleado', ADMIN: 'Administrador',
  AVAILABLE: 'Disponible', OCCUPIED: 'Ocupada', RESERVED: 'Reservada', MAINTENANCE: 'Mantenimiento', OUT_OF_SERVICE: 'Fuera de servicio',
  CONFIRMED: 'Confirmada', CHECKED_IN: 'Alojado', COMPLETED: 'Finalizada', CANCELLED: 'Cancelada',
  PENDING: 'Pendiente', IN_PROGRESS: 'En progreso', DONE: 'Completada',
  OPEN: 'Abierta', RESOLVED: 'Resuelta', INCOME: 'Ingreso', EXPENSE: 'Gasto'
};
const fmtDate = d => d ? new Date(`${d}T12:00:00`).toLocaleDateString('es-CO', {day: 'numeric', month: 'short'}) : '—';
const localDate = d => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
const today = () => localDate(new Date());
const future = (days = 1) => {const d = new Date(); d.setDate(d.getDate() + days); return localDate(d)};
const initials = name => name.split(' ').map(x => x[0]).slice(0, 2).join('');
/** Días restantes hasta una fecha; negativo si ya venció. */
const daysUntil = d => Math.ceil((new Date(`${d}T12:00:00`) - new Date()) / 86400000);

/* ---------- Tema claro / oscuro ---------- */
const useTheme = () => {
  const [theme, setTheme] = useState(() => localStorage.getItem('lindomar-theme') || 'light');
  useEffect(() => {
    const root = document.documentElement;
    // Se apagan las transiciones ANTES de cambiar el tema. El reflow forzado es
    // necesario: sin él el navegador agrupa ambos cambios y algunos elementos
    // se quedan pintados con los colores del tema anterior.
    root.classList.add('theme-switching');
    void root.offsetHeight;
    root.setAttribute('data-theme', theme);
    void root.offsetHeight;
    localStorage.setItem('lindomar-theme', theme);
    const raf = requestAnimationFrame(() => root.classList.remove('theme-switching'));
    return () => cancelAnimationFrame(raf);
  }, [theme]);
  const toggle = useCallback(() => setTheme(t => t === 'light' ? 'dark' : 'light'), []);
  return [theme, toggle];
};

function Toast({toast, onClose}) {
  useEffect(() => {
    if (toast) {const t = setTimeout(onClose, 3200); return () => clearTimeout(t)}
  }, [toast, onClose]);
  return toast ? <div className="toast"><CheckCircle2 size={18}/>{toast.text}</div> : null;
}

/* ---------- Autenticación ---------- */
function Login({onLogin}) {
  const [mode, setMode] = useState('login');
  const [form, setForm] = useState({email: 'huesped@lindomar.co', password: 'demo123'});
  const [signup, setSignup] = useState({name: '', email: '', password: '', phone: ''});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const submit = async e => {
    e.preventDefault();
    setLoading(true); setError('');
    try {
      const path = mode === 'login' ? '/auth/login' : '/auth/register';
      const payload = mode === 'login' ? form : signup;
      onLogin(await api(path, {method: 'POST', body: JSON.stringify(payload)}));
    } catch (err) {setError(err.message)} finally {setLoading(false)}
  };

  const demos = [['Huésped', 'huesped@lindomar.co'], ['Empleado', 'empleado@lindomar.co'], ['Administrador', 'admin@lindomar.co']];

  return <div className="login-page">
    <section className="login-story">
      <div className="brand light"><span className="brand-mark">L</span><span>LINDOMAR<small>HOTEL &amp; DESCANSO</small></span></div>
      <div className="story-copy">
        <span className="eyebrow">FRENTE AL MAR</span>
        <h1>Tu descanso,<br/><em>sin complicaciones</em></h1>
        <p>Reserva tu habitación, pide lo que necesites desde tu cuarto y resuelve tus dudas al instante. Todo desde un mismo lugar.</p>
        <div className="feature-row">
          <span><Waves/>Vista al mar</span>
          <span><Wifi/>Wi-Fi en todo el hotel</span>
          <span><ConciergeBell/>Atención inmediata</span>
          <span><Star/>Desayuno incluido</span>
        </div>
      </div>
    </section>

    <section className="login-panel">
      <form className="login-card" onSubmit={submit}>
        <div className="brand mobile-brand"><span className="brand-mark">L</span><span>LINDOMAR<small>HOTEL &amp; DESCANSO</small></span></div>
        <span className="eyebrow green">{mode === 'login' ? 'BIENVENIDO DE NUEVO' : 'CREAR CUENTA'}</span>
        <h1>{mode === 'login' ? 'Inicio de sesión' : 'Regístrate'}</h1>
        <p className="muted">{mode === 'login' ? 'Ingresa las credenciales de tu cuenta.' : 'Crea tu cuenta de huésped en menos de un minuto.'}</p>

        {mode === 'login' ? <>
          <label>Correo electrónico<input type="email" value={form.email} onChange={e => setForm({...form, email: e.target.value})} required/></label>
          <label>Contraseña<input type="password" value={form.password} onChange={e => setForm({...form, password: e.target.value})} required/></label>
        </> : <>
          <label>Nombre completo<input value={signup.name} onChange={e => setSignup({...signup, name: e.target.value})} placeholder="Ej: María Restrepo" required/></label>
          <label>Correo electrónico<input type="email" value={signup.email} onChange={e => setSignup({...signup, email: e.target.value})} placeholder="tucorreo@ejemplo.com" required/></label>
          <label>Teléfono<input value={signup.phone} onChange={e => setSignup({...signup, phone: e.target.value})} placeholder="300 000 0000"/></label>
          <label>Contraseña<input type="password" value={signup.password} onChange={e => setSignup({...signup, password: e.target.value})} placeholder="Mínimo 6 caracteres" required/></label>
        </>}

        {error && <p className="form-error">{error}</p>}
        <button className="primary wide" disabled={loading}>
          {loading ? 'Un momento…' : mode === 'login' ? 'Ingresar' : <><UserPlus size={17}/>Crear mi cuenta</>}
        </button>

        <p className="auth-switch">
          {mode === 'login' ? '¿Aún no tienes cuenta?' : '¿Ya tienes una cuenta?'}
          <button type="button" onClick={() => {setMode(mode === 'login' ? 'register' : 'login'); setError('')}}>
            {mode === 'login' ? 'Regístrate aquí' : 'Inicia sesión'}
          </button>
        </p>

        {mode === 'login' && <div className="demo">
          <span>Cuentas de demostración</span>
          <div>{demos.map(([name, email]) => <button type="button" key={email} onClick={() => setForm({email, password: 'demo123'})}>{name}</button>)}</div>
          <small>Contraseña: demo123</small>
        </div>}
      </form>
    </section>
  </div>;
}

/* ---------- Navegación por rol ---------- */
const navByRole = {
  GUEST: [
    ['home', 'Inicio', LayoutDashboard], ['rooms', 'Habitaciones', BedDouble],
    ['reservations', 'Mis reservas', CalendarDays], ['requests', 'Atención', ConciergeBell],
    ['assistant', 'Asistente', MessageCircle]
  ],
  EMPLOYEE: [
    ['tasks', 'Mis tareas', ClipboardCheck], ['staff-requests', 'Solicitudes', ConciergeBell]
  ],
  ADMIN: [
    ['admin-home', 'Resumen', LayoutDashboard], ['admin-rooms', 'Habitaciones', BedDouble],
    ['admin-reservations', 'Reservas', CalendarDays], ['admin-requests', 'Solicitudes', ConciergeBell],
    ['admin-users', 'Equipo y usuarios', Users], ['admin-tasks', 'Tareas', ClipboardCheck],
    ['finance', 'Finanzas', CircleDollarSign]
  ]
};

function Shell({user, onLogout, theme, toggleTheme}) {
  const [page, setPage] = useState(navByRole[user.role][0][0]);
  const [open, setOpen] = useState(false);
  const [toast, setToast] = useState(null);
  const [openRequests, setOpenRequests] = useState(0);
  const notify = useCallback(text => setToast({text}), []);

  // Contador de solicitudes abiertas para el badge del menú (roles de staff).
  const refreshBadge = useCallback(() => {
    if (user.role === 'GUEST') return;
    api('/requests').then(list => setOpenRequests(list.filter(r => r.status !== 'RESOLVED').length)).catch(() => {});
  }, [user.role]);
  useEffect(() => {refreshBadge()}, [refreshBadge, page]);

  return <div className="app-shell">
    <aside className={open ? 'open' : ''}>
      <div className="brand light"><span className="brand-mark">L</span><span>LINDOMAR<small>HOTEL &amp; DESCANSO</small></span></div>
      <button className="close-menu" onClick={() => setOpen(false)}><X size={18}/></button>
      <nav>{navByRole[user.role].map(([id, name, Icon]) =>
        <button key={id} className={page === id ? 'active' : ''} onClick={() => {setPage(id); setOpen(false)}}>
          <Icon size={19}/>{name}
          {id.includes('requests') && openRequests > 0 && <span className="nav-badge">{openRequests}</span>}
        </button>)}
      </nav>
      <div className="sidebar-foot">
        <div className="avatar">{initials(user.name)}</div>
        <div><strong>{user.name}</strong><small>{label[user.role]}</small></div>
        <button title="Cerrar sesión" onClick={onLogout}><LogOut size={18}/></button>
      </div>
    </aside>

    <main>
      <header>
        <button className="menu-button" onClick={() => setOpen(true)}><Menu/></button>
        <div>
          <span className="eyebrow green">HOTEL LINDOMAR</span>
          <h2>{navByRole[user.role].find(x => x[0] === page)?.[1]}</h2>
        </div>
        <div className="header-actions">
          <span className="role-chip"><ShieldCheck size={15}/>{label[user.role]}</span>
          <button className="theme-toggle" onClick={toggleTheme} title={theme === 'light' ? 'Modo oscuro' : 'Modo claro'}>
            {theme === 'light' ? <Moon/> : <Sun/>}
          </button>
          <div className="avatar">{user.name[0]}</div>
        </div>
      </header>
      <div className="content">
        {user.role === 'GUEST' ? <Guest page={page} user={user} notify={notify} go={setPage}/>
          : user.role === 'EMPLOYEE' ? <Employee page={page} user={user} notify={notify} onChange={refreshBadge}/>
          : <Admin page={page} notify={notify} onChange={refreshBadge}/>}
      </div>
    </main>
    <Toast toast={toast} onClose={() => setToast(null)}/>
  </div>;
}

/* ============ HUÉSPED ============ */
function Guest({page, user, notify, go}) {
  if (page === 'home') return <GuestHome user={user} go={go}/>;
  if (page === 'rooms') return <Rooms user={user} notify={notify}/>;
  if (page === 'reservations') return <Reservations user={user} notify={notify}/>;
  if (page === 'requests') return <Requests user={user} notify={notify}/>;
  return <Assistant/>;
}

function GuestHome({user, go}) {
  const [reservations, setReservations] = useState([]);
  useEffect(() => {api('/reservations/mine').then(setReservations)}, [user.id]);
  const next = reservations.find(r => r.status === 'CONFIRMED' || r.status === 'CHECKED_IN');

  return <>
    <section className="welcome">
      <div>
        <span className="eyebrow">CUENTA DE HUÉSPED</span>
        <h1>Hola, {user.name.split(' ')[0]}</h1>
        <p>Consulta tus reservas, pide atención a tu habitación o resuelve tus dudas con el asistente.</p>
        <button className="primary" onClick={() => go('rooms')}><Search size={18}/>Buscar habitación</button>
      </div>
    </section>

    <div className="section-head"><div><h3>Tu próxima estadía</h3><p className="muted">Información registrada en el sistema.</p></div></div>
    {next ? <div className="stay-card">
      <div className="stay-number"><small>HABITACIÓN</small><strong>{next.roomNumber}</strong><span>{next.roomType}</span></div>
      <div className="stay-detail">
        <span><CalendarDays/>Llegada<strong>{fmtDate(next.checkIn)}</strong></span>
        <span><CalendarDays/>Salida<strong>{fmtDate(next.checkOut)}</strong></span>
        <span><Users/>Huéspedes<strong>{next.guests}</strong></span>
      </div>
      <span className="status confirmed">{label[next.status]}</span>
    </div> : <div className="empty">
      <BedDouble/><h3>No tienes reservas activas</h3><p>Explora las habitaciones disponibles y reserva la que más te guste.</p>
      <button className="primary" onClick={() => go('rooms')}>Consultar habitaciones</button>
    </div>}

    <div className="quick-grid">
      <button onClick={() => go('rooms')}><span><BedDouble/></span><strong>Buscar habitación</strong><small>Disponibilidad y filtros</small></button>
      <button onClick={() => go('requests')}><span><ConciergeBell/></span><strong>Solicitar atención</strong><small>Pide algo a tu habitación</small></button>
      <button onClick={() => go('assistant')}><span><MessageCircle/></span><strong>Consultar asistente</strong><small>Horarios, servicios y más</small></button>
    </div>
  </>;
}

function Rooms({user, notify}) {
  const [rooms, setRooms] = useState([]);
  const [allRooms, setAllRooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [mapView, setMapView] = useState(false);
  const [selected, setSelected] = useState(null);
  const [filters, setFilters] = useState({type: '', capacity: '', maxPrice: '', balcony: false, petFriendly: false, checkIn: today(), checkOut: future(1)});
  const [searchError, setSearchError] = useState('');
  const [searchedDates, setSearchedDates] = useState(null);
  const searchVersion = useRef(0);

  const load = async () => {
    const version = ++searchVersion.current;
    setSearchError('');setRooms([]);setSearchedDates(null);
    if (!filters.checkIn || !filters.checkOut || filters.checkIn < today() || filters.checkOut <= filters.checkIn) {
      setSearchError('Selecciona una llegada desde hoy y una salida posterior a la llegada.');
      setLoading(false);return;
    }
    setLoading(true);
    const p = new URLSearchParams();
    Object.entries(filters).forEach(([k, v]) => v !== '' && v !== false && p.set(k, v));
    try {
      const results = await api(`/rooms?${p}`);
      if (version === searchVersion.current) {setRooms(results);setSearchedDates({checkIn: filters.checkIn, checkOut: filters.checkOut})}
    } catch (err) {if (version === searchVersion.current) setSearchError(err.message)}
    finally {if (version === searchVersion.current) setLoading(false)}
  };
  useEffect(() => {load(); api('/rooms/all').then(setAllRooms)}, []);

  return <>
    <div className="section-head">
      <div><h3>Encuentra tu habitación</h3><p className="muted">Elige las fechas y preferencias de tu estadía.</p></div>
      <div className="view-toggle">
        <button className={!mapView ? 'active' : ''} onClick={() => setMapView(false)}><BedDouble/>Lista</button>
        <button className={mapView ? 'active' : ''} onClick={() => setMapView(true)}><Map/>Mapa</button>
      </div>
    </div>

    <div className="filter-card">
      <label>Llegada<input type="date" min={today()} value={filters.checkIn} onChange={e => setFilters({...filters, checkIn: e.target.value})}/></label>
      <label>Salida<input type="date" min={filters.checkIn} value={filters.checkOut} onChange={e => setFilters({...filters, checkOut: e.target.value})}/></label>
      <label>Tipo<select value={filters.type} onChange={e => setFilters({...filters, type: e.target.value})}>
        <option value="">Todos</option><option>Estándar</option><option>Familiar</option><option>Deluxe</option><option>Suite</option>
      </select></label>
      <label>Personas<select value={filters.capacity} onChange={e => setFilters({...filters, capacity: e.target.value})}>
        <option value="">Todas</option><option value="2">2+</option><option value="3">3+</option><option value="4">4+</option>
      </select></label>
      <label>Precio máximo<select value={filters.maxPrice} onChange={e => setFilters({...filters, maxPrice: e.target.value})}>
        <option value="">Sin límite</option><option value="250000">$250.000</option><option value="350000">$350.000</option><option value="500000">$500.000</option>
      </select></label>
      <div className="checks">
        <label><input type="checkbox" checked={filters.balcony} onChange={e => setFilters({...filters, balcony: e.target.checked})}/>Balcón</label>
        <label><input type="checkbox" checked={filters.petFriendly} onChange={e => setFilters({...filters, petFriendly: e.target.checked})}/>Mascotas</label>
      </div>
      <button className="primary search-button" onClick={load}><Search/>Buscar</button>
    </div>

    {searchError ? <p className="form-error" role="alert">{searchError}</p>
      : loading ? <div className="loader">Buscando disponibilidad…</div>
      : mapView ? <RoomMap rooms={rooms} allRooms={allRooms} onSelect={setSelected}/>
      : <div className="room-grid">
          {rooms.map(r => <RoomCard key={r.id} room={r} onSelect={setSelected}/>)}
          {!rooms.length && <div className="empty span-all">
            <BedDouble/><h3>No encontramos habitaciones</h3><p>Prueba quitando algún filtro o cambiando las fechas.</p>
          </div>}
        </div>}

    {selected && searchedDates && <ReserveModal room={selected} user={user} dates={searchedDates} onClose={() => setSelected(null)}
      onDone={() => {setSelected(null); load(); notify('Reserva confirmada. ¡Te esperamos!')}}/>}
  </>;
}

function RoomCard({room, onSelect}) {
  const typeClass = room.type.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
  return <article className="room-card">
    <div className={`room-visual type-${typeClass}`}>
      <span>{room.type}</span><BedDouble size={54}/><small>Habitación {room.number}</small>
    </div>
    <div className="room-body">
      <div><h3>{room.type}</h3><span className="status available">Disponible</span></div>
      <p><Users size={16}/>Hasta {room.capacity} huéspedes</p>
      <div className="tags">
        {room.balcony && <span>Balcón</span>}
        {room.petFriendly && <span>Pet-friendly</span>}
        {(room.services || '').split(' · ').filter(Boolean).map(s => <span key={s}>{s}</span>)}
      </div>
      <div className="room-foot">
        <span><strong>{money(room.price)}</strong><small>por noche</small></span>
        <button className="primary" onClick={() => onSelect(room)}>Ver y reservar</button>
      </div>
    </div>
  </article>;
}

/** Mapa tipo cine: construido a partir de las habitaciones reales del hotel. */
function RoomMap({rooms, allRooms, onSelect}) {
  const source = allRooms.length ? allRooms : rooms;
  const floors = [...new Set(source.map(r => r.floor))].sort((a, b) => b - a);
  const availableIds = new Set(rooms.map(r => r.id));

  return <div className="hotel-map">
    <div className="map-legend">
      <span><i/>Disponible</span><span><i className="occupied"/>No disponible</span>
    </div>
    {floors.map(f => <div className="floor" key={f}>
      <strong>Piso {f}</strong>
      <div>{source.filter(r => r.floor === f).sort((a, b) => a.number.localeCompare(b.number)).map(room => {
        const free = availableIds.has(room.id);
        return <button key={room.id} className={free ? 'available' : 'occupied'} disabled={!free}
          onClick={() => free && onSelect(room)}>
          <DoorOpen/><span>{room.number}</span>
          <small>{free ? money(room.price) : room.status === 'AVAILABLE' ? 'No disponible en estas fechas' : label[room.status] || 'No disponible'}</small>
        </button>;
      })}</div>
    </div>)}
  </div>;
}

function ReserveModal({room, user, dates, onClose, onDone}) {
  const [form, setForm] = useState({roomId: room.id, checkIn: dates.checkIn, checkOut: dates.checkOut, guests: 1, notes: ''});
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const nights = Math.max(1, Math.ceil((new Date(form.checkOut) - new Date(form.checkIn)) / 86400000));

  const submit = async e => {
    e.preventDefault(); setBusy(true); setError('');
    try {await api('/reservations', {method: 'POST', body: JSON.stringify(form)}); onDone()}
    catch (err) {setError(err.message)} finally {setBusy(false)}
  };

  return <div className="modal-backdrop" onMouseDown={e => e.target === e.currentTarget && onClose()}>
    <form className="modal" onSubmit={submit}>
      <button type="button" className="modal-close" onClick={onClose}><X/></button>
      <span className="eyebrow green">CONFIRMAR RESERVA</span>
      <h2>Habitación {room.number}</h2>
      <p className="muted">{room.type} · hasta {room.capacity} huéspedes</p>
      <div className="tags" aria-label="Características y servicios">
        {room.balcony && <span>Balcón</span>}
        {room.petFriendly && <span>Pet-friendly</span>}
        {(room.services || '').split(' · ').filter(Boolean).map(service => <span key={service}>{service}</span>)}
      </div>
      <div className="form-grid">
        <label>Llegada<input type="date" min={today()} value={form.checkIn} onChange={e => setForm({...form, checkIn: e.target.value})}/></label>
        <label>Salida<input type="date" min={form.checkIn} value={form.checkOut} onChange={e => setForm({...form, checkOut: e.target.value})}/></label>
        <label>Huéspedes<select value={form.guests} onChange={e => setForm({...form, guests: Number(e.target.value)})}>
          {Array.from({length: room.capacity}, (_, i) => <option key={i + 1}>{i + 1}</option>)}
        </select></label>
        <label className="full">Observaciones<textarea placeholder="Ej: cuna para bebé, llegada tarde…" value={form.notes} onChange={e => setForm({...form, notes: e.target.value})}/></label>
      </div>
      <div className="price-summary">
        <span>{nights} {nights === 1 ? 'noche' : 'noches'} × {money(room.price)}</span>
        <strong>{money(room.price * nights)}</strong>
      </div>
      {error && <p className="form-error">{error}</p>}
      <button className="primary wide" disabled={busy}>{busy ? 'Confirmando…' : 'Confirmar reserva'}</button>
    </form>
  </div>;
}

function Reservations({user, notify}) {
  const [items, setItems] = useState([]);
  const load = () => api('/reservations/mine').then(setItems);
  // Se envuelve en llaves a propósito: si el efecto devuelve la promesa de `load`,
  // React la toma como función de limpieza y falla al desmontar la vista.
  useEffect(() => {load()}, [user.id]);
  const cancel = async id => {
    if (!confirm('¿Deseas cancelar esta reserva?')) return;
    await api(`/reservations/${id}`, {method: 'DELETE'});
    notify('Reserva cancelada'); load();
  };

  return <>
    <div className="section-head"><div><h3>Mis reservas</h3><p className="muted">Consulta el estado y los detalles de tus estadías.</p></div></div>
    <div className="list-stack">
      {items.map(r => <article className="reservation-row" key={r.id}>
        <div className="room-badge"><BedDouble/><strong>{r.roomNumber}</strong></div>
        <div>
          <h3>{r.roomType}</h3>
          <p>{fmtDate(r.checkIn)} — {fmtDate(r.checkOut)} · {r.guests} huésped(es)</p>
          {r.notes && <small>“{r.notes}”</small>}
        </div>
        <strong className="reservation-total">{money(r.total)}</strong>
        <span className={`status ${r.status.toLowerCase()}`}>{label[r.status]}</span>
        {r.status === 'CONFIRMED' && <button className="ghost danger" onClick={() => cancel(r.id)}>Cancelar</button>}
      </article>)}
      {!items.length && <div className="empty"><CalendarDays/><h3>No tienes reservas todavía</h3></div>}
    </div>
  </>;
}

function Requests({user, notify}) {
  const [items, setItems] = useState([]);
  const [form, setForm] = useState({guestId: user.id, roomNumber: '', category: 'Toallas y lencería', notes: ''});
  const load = () => api(`/requests?guestId=${user.id}`).then(setItems);
  // Se envuelve en llaves a propósito: si el efecto devuelve la promesa de `load`,
  // React la toma como función de limpieza y falla al desmontar la vista.
  useEffect(() => {load()}, [user.id]);
  const submit = async e => {
    e.preventDefault();
    await api('/requests', {method: 'POST', body: JSON.stringify(form)});
    setForm({...form, notes: ''}); notify('Solicitud enviada al equipo'); load();
  };

  return <div className="two-column">
    <form className="panel request-form" onSubmit={submit}>
      <span className="eyebrow green">ATENCIÓN A LA HABITACIÓN</span>
      <h3>¿En qué podemos ayudarte?</h3>
      <label>Habitación<input placeholder="Ej: 201" value={form.roomNumber} onChange={e => setForm({...form, roomNumber: e.target.value})} required/></label>
      <label>Tipo de solicitud<select value={form.category} onChange={e => setForm({...form, category: e.target.value})}>
        <option>Toallas y lencería</option><option>Limpieza</option><option>Alimentos y bebidas</option><option>Mantenimiento</option><option>Otro</option>
      </select></label>
      <label>Observaciones<textarea placeholder="Cuéntanos qué necesitas…" value={form.notes} onChange={e => setForm({...form, notes: e.target.value})} required/></label>
      <button className="primary wide"><ConciergeBell size={17}/>Enviar solicitud</button>
    </form>
    <div>
      <div className="section-head"><div><h3>Solicitudes recientes</h3><p className="muted">Sigue el progreso de tu atención.</p></div></div>
      <div className="list-stack compact">
        {items.map(i => <article className="request-row" key={i.id}>
          <span><ConciergeBell/></span>
          <div>
            <strong>{i.category}</strong>
            <small>Habitación {i.roomNumber} · {new Date(i.createdAt).toLocaleString('es-CO', {dateStyle: 'medium', timeStyle: 'short'})}</small>
            <p>{i.notes}</p>
          </div>
          <em className={`status ${i.status.toLowerCase()}`}>{label[i.status]}</em>
        </article>)}
        {!items.length && <div className="empty small"><ConciergeBell/><p>Aquí aparecerán tus solicitudes.</p></div>}
      </div>
    </div>
  </div>;
}

function Assistant() {
  const [messages, setMessages] = useState([{from: 'bot', text: '¡Hola! Soy el asistente Lindomar. Pregúntame por horarios, servicios o tu estadía.'}]);
  const [q, setQ] = useState('');
  const ask = async text => {
    const question = text || q;
    if (!question.trim()) return;
    setMessages(m => [...m, {from: 'user', text: question}]); setQ('');
    const r = await api(`/assistant?q=${encodeURIComponent(question)}`);
    setMessages(m => [...m, {from: 'bot', text: r.answer}]);
  };
  const suggestions = ['¿Cuál es el horario del desayuno?', '¿Aceptan mascotas?', '¿A qué hora es el check-in?', '¿La piscina está abierta?'];

  return <div className="chat-card">
    <div className="chat-head">
      <span><Sparkles/></span>
      <div><h3>Asistente Lindomar</h3><small>En línea · respuesta inmediata</small></div>
    </div>
    <div className="messages">{messages.map((m, i) => <div key={i} className={`message ${m.from}`}>{m.text}</div>)}</div>
    <div className="suggestions">{suggestions.map(s => <button key={s} onClick={() => ask(s)}>{s}</button>)}</div>
    <form className="chat-input" onSubmit={e => {e.preventDefault(); ask()}}>
      <input value={q} onChange={e => setQ(e.target.value)} placeholder="Escribe tu pregunta…"/>
      <button><MessageCircle/></button>
    </form>
  </div>;
}

/* ============ EMPLEADO ============ */
function Employee({page, user, notify, onChange}) {
  if (page === 'staff-requests') return <ServiceRequests notify={notify} onChange={onChange}/>;
  return <EmployeeTasks user={user} notify={notify}/>;
}

function EmployeeTasks({user, notify}) {
  const [tasks, setTasks] = useState([]);
  const load = () => api(`/tasks?employeeId=${user.id}`).then(setTasks);
  // Se envuelve en llaves a propósito: si el efecto devuelve la promesa de `load`,
  // React la toma como función de limpieza y falla al desmontar la vista.
  useEffect(() => {load()}, [user.id]);
  const update = async (id, status) => {
    await api(`/tasks/${id}`, {method: 'PUT', body: JSON.stringify({status})});
    notify('Tarea actualizada'); load();
  };
  const complete = async (id, photo) => {
    const body = new FormData();
    if (photo) body.append('photo', photo);
    await api(`/tasks/${id}/complete`, {method: 'POST', body});
    notify(photo ? 'Tarea completada con evidencia fotográfica' : 'Tarea completada sin fotografía');
    load();
  };
  const done = tasks.filter(t => t.status === 'DONE').length;

  return <>
    <section className="employee-hero">
      <div>
        <span className="eyebrow">CUENTA DE EMPLEADO</span>
        <h1>{user.name}</h1>
        <p>Estado de las tareas asignadas para el turno actual.</p>
      </div>
      <div className="progress-ring">
        <strong>{tasks.length ? Math.round(done / tasks.length * 100) : 0}%</strong><span>completado</span>
      </div>
    </section>

    <div className="metric-grid three">
      <Metric icon={ClipboardCheck} label="Pendientes" value={tasks.filter(t => t.status === 'PENDING').length}/>
      <Metric icon={Clock3} label="En progreso" value={tasks.filter(t => t.status === 'IN_PROGRESS').length}/>
      <Metric icon={CheckCircle2} label="Completadas" value={done}/>
    </div>

    <div className="section-head">
      <div><h3>Tareas asignadas</h3><p className="muted">Actualiza el estado de cada actividad.</p></div>
      <span className="notification-chip">{tasks.filter(t => t.status === 'PENDING').length} pendientes</span>
    </div>
    <div className="task-board">
      {['PENDING', 'IN_PROGRESS', 'DONE'].map(status => <section key={status}>
        <h4>{label[status]} <span>{tasks.filter(t => t.status === status).length}</span></h4>
        {tasks.filter(t => t.status === status).map(t => <article className="task-card" key={t.id}>
          <div><span className={`priority ${t.priority.toLowerCase()}`}>{t.priority}</span><small>{fmtDate(t.dueDate)}</small></div>
          <h3>{t.title}</h3><p>{t.description}</p>
          {status === 'PENDING' ? <button className="secondary" onClick={() => update(t.id, 'IN_PROGRESS')}>Iniciar tarea</button>
            : status === 'IN_PROGRESS' ? <TaskCompletion task={t} onComplete={complete}/>
            : <><span className="done-mark"><CheckCircle2/>Finalizada</span>
              {t.hasCompletionPhoto && <span className="evidence-sent"><Camera size={14}/>Evidencia enviada</span>}</>}
        </article>)}
      </section>)}
    </div>
  </>;
}

/** HU-38: la fotografía es opcional y se envía junto con el cierre de la tarea. */
function TaskCompletion({task, onComplete}) {
  const [photo, setPhoto] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const choose = e => {
    const file = e.target.files?.[0] || null;
    if (file && file.size > 5 * 1024 * 1024) {setError('La fotografía no puede superar 5 MB'); setPhoto(null); return}
    setError(''); setPhoto(file);
  };
  const finish = async () => {
    setBusy(true); setError('');
    try {await onComplete(task.id, photo)} catch (err) {setError(err.message)} finally {setBusy(false)}
  };
  return <div className="task-completion">
    <label className="photo-picker">
      <Camera size={16}/><span>{photo ? photo.name : 'Adjuntar fotografía (opcional)'}</span>
      <input type="file" accept="image/*" capture="environment" onChange={choose}/>
    </label>
    {error && <small className="form-error">{error}</small>}
    <button className="primary" onClick={finish} disabled={busy}><CheckCircle2/>{busy ? 'Enviando…' : 'Completar tarea'}</button>
  </div>;
}

/**
 * HU-17: bandeja de solicitudes de atención para el personal.
 * Cierra el ciclo del botón de atención del huésped.
 */
function ServiceRequests({notify, onChange}) {
  const [items, setItems] = useState([]);
  const load = () => api('/requests').then(list =>
    setItems([...list].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))));
  useEffect(() => {load()}, []);

  const update = async (id, status) => {
    await api(`/requests/${id}`, {method: 'PUT', body: JSON.stringify({status})});
    notify(status === 'RESOLVED' ? 'Solicitud marcada como resuelta' : 'Solicitud en progreso');
    load(); onChange?.();
  };

  const open = items.filter(i => i.status !== 'RESOLVED');
  const resolved = items.filter(i => i.status === 'RESOLVED');

  return <>
    <div className="metric-grid three">
      <Metric icon={ConciergeBell} label="Abiertas" value={items.filter(i => i.status === 'OPEN').length}/>
      <Metric icon={Clock3} label="En progreso" value={items.filter(i => i.status === 'IN_PROGRESS').length}/>
      <Metric icon={CheckCircle2} label="Resueltas" value={resolved.length}/>
    </div>

    <div className="section-head">
      <div><h3>Solicitudes por atender</h3><p className="muted">Peticiones enviadas por los huéspedes desde sus habitaciones.</p></div>
      {open.length > 0 && <span className="notification-chip"><BellRing size={15}/>{open.length} sin resolver</span>}
    </div>
    <div className="list-stack">
      {open.map(i => <article className="request-row" key={i.id}>
        <span><ConciergeBell/></span>
        <div>
          <strong>{i.category} · Habitación {i.roomNumber}</strong>
          <small>{new Date(i.createdAt).toLocaleString('es-CO', {dateStyle: 'medium', timeStyle: 'short'})}</small>
          <p>{i.notes}</p>
        </div>
        <div className="request-actions">
          <span className={`status ${i.status.toLowerCase()}`}>{label[i.status]}</span>
          {i.status === 'OPEN' && <button className="secondary" onClick={() => update(i.id, 'IN_PROGRESS')}>Atender</button>}
          {i.status === 'IN_PROGRESS' && <button className="primary" onClick={() => update(i.id, 'RESOLVED')}><CheckCircle2 size={16}/>Resolver</button>}
        </div>
      </article>)}
      {!open.length && <div className="empty"><CheckCircle2/><h3>Todo al día</h3><p>No hay solicitudes pendientes por atender.</p></div>}
    </div>

    {resolved.length > 0 && <>
      <div className="section-head"><div><h3>Resueltas recientemente</h3></div></div>
      <div className="list-stack compact">
        {resolved.slice(0, 5).map(i => <article className="request-row" key={i.id}>
          <span><CheckCircle2/></span>
          <div><strong>{i.category} · Habitación {i.roomNumber}</strong><p>{i.notes}</p></div>
          <span className="status resolved">{label[i.status]}</span>
        </article>)}
      </div>
    </>}
  </>;
}

/* ============ ADMINISTRADOR ============ */
function Metric({icon: Icon, label: lab, value, help}) {
  return <div className="metric">
    <span><Icon/></span>
    <div><small>{lab}</small><strong>{value}</strong>{help && <em>{help}</em>}</div>
  </div>;
}

function Admin({page, notify, onChange}) {
  if (page === 'admin-home') return <AdminHome notify={notify}/>;
  if (page === 'admin-rooms') return <AdminRooms notify={notify}/>;
  if (page === 'admin-reservations') return <AdminReservations notify={notify}/>;
  if (page === 'admin-requests') return <ServiceRequests notify={notify} onChange={onChange}/>;
  if (page === 'admin-users') return <AdminUsers/>;
  if (page === 'admin-tasks') return <AdminTasks notify={notify}/>;
  return <Finance notify={notify}/>;
}

function AdminHome({notify}) {
  const [d, setD] = useState(null);
  const [reminders, setReminders] = useState([]);
  const load = () => {api('/dashboard').then(setD); api('/reminders').then(setReminders)};
  useEffect(() => {load()}, []);
  if (!d) return <div className="loader">Cargando indicadores…</div>;

  const occupancy = Math.round(d.occupied / d.rooms * 100);
  const pending = reminders.filter(r => r.status === 'PENDING');

  const pay = async r => {
    await api(`/reminders/${r.id}/pay`, {method: 'POST'});
    notify('Gasto registrado y descontado del balance'); load();
  };

  return <>
    <section className="admin-welcome">
      <div>
        <span className="eyebrow">ADMINISTRACIÓN</span>
        <h1>Resumen operativo</h1>
        <p>Todo lo que está pasando hoy en el hotel, en una sola pantalla.</p>
      </div>
      <span>{new Date().toLocaleDateString('es-CO', {weekday: 'long', day: 'numeric', month: 'long'})}</span>
    </section>

    <div className="metric-grid four">
      <Metric icon={BedDouble} label="Ocupación actual" value={`${occupancy}%`} help={`${d.occupied} de ${d.rooms} habitaciones`}/>
      <Metric icon={CalendarDays} label="Reservas activas" value={d.activeReservations} help="Confirmadas y alojadas"/>
      <Metric icon={ConciergeBell} label="Solicitudes abiertas" value={d.openRequests} help="Huéspedes esperando atención"/>
      <Metric icon={WalletCards} label="Balance" value={money(d.balance)} help="Ingresos menos gastos"/>
    </div>

    <div className="insight-grid">
      <div className="panel occupancy">
        <div className="section-head" style={{marginTop: 0}}>
          <div><h3>Ocupación</h3><p className="muted">Distribución actual del hotel.</p></div>
        </div>
        <div className="big-progress"><div style={{width: `${occupancy}%`}}/><span>{occupancy}%</span></div>
        <div className="occupancy-stats">
          <span><i/>{d.rooms - d.occupied} disponibles</span>
          <span><i className="occupied"/>{d.occupied} ocupadas</span>
        </div>
      </div>
      <div className="panel recommendation">
        <span><Users/></span>
        <div>
          <small>ESTIMACIÓN DE PERSONAL</small>
          <h3>{d.suggestedCleaners} persona(s) de aseo</h3>
          <p>Calculado según la ocupación y las reservas activas.</p>
        </div>
      </div>
    </div>

    <div className="section-head">
      <div><h3>Recordatorios de gastos</h3><p className="muted">Pagos y compras necesarias para que la operación no se detenga.</p></div>
      {d.remindersDueSoon > 0 && <span className="notification-chip"><BellRing size={15}/>{d.remindersDueSoon} próximos a vencer</span>}
    </div>
    <div className="reminder-list">
      {pending.map(r => {
        const days = daysUntil(r.dueDate);
        const urgent = days <= 7;
        return <div className={`reminder ${urgent ? 'urgent' : ''}`} key={r.id}>
          <span>{urgent ? <BellRing size={18}/> : <Bell size={18}/>}</span>
          <div>
            <strong>{r.concept}</strong>
            <small>{r.category} · {money(r.estimatedAmount)} · {days < 0 ? `vencido hace ${Math.abs(days)} día(s)` : days === 0 ? 'vence hoy' : `en ${days} día(s)`}</small>
          </div>
          <button className="secondary" onClick={() => pay(r)}>Registrar gasto</button>
        </div>;
      })}
      {!pending.length && <div className="empty small"><CheckCircle2/><p>No hay gastos pendientes programados.</p></div>}
    </div>
  </>;
}

const EMPTY_ROOM = {number: '', floor: 1, type: 'Estándar', capacity: 2, price: 200000, balcony: false, petFriendly: false, services: 'Wi-Fi · TV', status: 'AVAILABLE'};

function AdminRooms({notify}) {
  const [rooms, setRooms] = useState([]);
  const [editing, setEditing] = useState(null);
  const [creating, setCreating] = useState(false);
  const [statusError, setStatusError] = useState('');
  const [updating, setUpdating] = useState(null);
  const load = () => api('/rooms/all').then(setRooms);
  useEffect(() => {load()}, []);

  const save = async room => {
    await api(`/rooms/${room.id}`, {method: 'PUT', body: JSON.stringify(room)});
    setEditing(null); notify('Habitación actualizada'); load();
  };
  const changeStatus = async (room, status) => {
    setStatusError('');setUpdating(room.id);
    try {
      const updated = await api(`/rooms/${room.id}/status`, {method: 'PATCH', body: JSON.stringify({status})});
      setRooms(items => items.map(item => item.id === updated.id ? updated : item));
      notify(`Habitación ${updated.number}: ${label[updated.status]}`);
    } catch (err) {setStatusError(err.message)}
    finally {setUpdating(null)}
  };
  const create = async room => {
    await api('/rooms', {method: 'POST', body: JSON.stringify(room)});
    setCreating(false); notify('Habitación creada'); load();
  };
  const remove = async room => {
    if (!confirm(`¿Eliminar la habitación ${room.number}? Esta acción no se puede deshacer.`)) return;
    await api(`/rooms/${room.id}`, {method: 'DELETE'});
    notify('Habitación eliminada'); load();
  };

  return <>
    <div className="section-head">
      <div><h3>Inventario de habitaciones</h3><p className="muted">Actualiza el estado operativo y la información principal.</p></div>
      <button className="primary" onClick={() => setCreating(true)}><Plus size={17}/>Nueva habitación</button>
    </div>
    {statusError && <p className="form-error" role="alert">{statusError}</p>}
    <div className="table-wrap"><table>
      <thead><tr><th>Habitación</th><th>Tipo</th><th>Capacidad</th><th>Precio/noche</th><th>Características</th><th>Estado</th><th></th></tr></thead>
      <tbody>{rooms.map(r => <tr key={r.id}>
        <td><strong>{r.number}</strong><small>Piso {r.floor}</small></td>
        <td>{r.type}</td><td>{r.capacity} personas</td><td>{money(r.price)}</td>
        <td><div className="tags">{r.balcony && <span>Balcón</span>}{r.petFriendly && <span>Mascotas</span>}</div></td>
        <td><select aria-label={`Estado de habitación ${r.number}`} value={r.status} disabled={updating !== null} onChange={e => changeStatus(r, e.target.value)}>
          {['AVAILABLE', 'OCCUPIED', 'RESERVED', 'MAINTENANCE', 'OUT_OF_SERVICE'].map(status => <option key={status} value={status}>{label[status]}</option>)}
        </select></td>
        <td><div className="request-actions">
          <button className="icon-button" title="Editar" onClick={() => setEditing({...r})}><Settings2/></button>
          <button className="icon-button danger" title="Eliminar" onClick={() => remove(r)}><Trash2/></button>
        </div></td>
      </tr>)}</tbody>
    </table></div>

    {(editing || creating) && <RoomModal
      room={editing || {...EMPTY_ROOM}}
      isNew={creating}
      onClose={() => {setEditing(null); setCreating(false)}}
      onSave={creating ? create : save}/>}
  </>;
}

function RoomModal({room, isNew, onClose, onSave}) {
  const [data, setData] = useState(room);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k, v) => setData(d => ({...d, [k]: v}));
  return <div className="modal-backdrop" onMouseDown={e => e.target === e.currentTarget && onClose()}>
    <form className="modal" onSubmit={async e => {e.preventDefault();setError('');setBusy(true);try {await onSave(data)} catch(err) {setError(err.message)} finally {setBusy(false)}}}>
      <button type="button" className="modal-close" onClick={onClose}><X/></button>
      <span className="eyebrow green">{isNew ? 'NUEVA HABITACIÓN' : 'EDITAR HABITACIÓN'}</span>
      <h2>{isNew ? 'Registrar habitación' : `Habitación ${data.number}`}</h2>
      <div className="form-grid">
        <label>Número<input value={data.number} onChange={e => set('number', e.target.value)} placeholder="Ej: 204" required/></label>
        <label>Piso<input type="number" min="1" value={data.floor} onChange={e => set('floor', Number(e.target.value))}/></label>
        <label>Tipo<select value={data.type} onChange={e => set('type', e.target.value)}>
          <option>Estándar</option><option>Familiar</option><option>Deluxe</option><option>Suite</option>
        </select></label>
        <label>Capacidad<input type="number" min="1" value={data.capacity} onChange={e => set('capacity', Number(e.target.value))}/></label>
        <label>Precio por noche<input type="number" min="0" value={data.price} onChange={e => set('price', Number(e.target.value))}/></label>
        <label>Estado<select value={data.status} onChange={e => set('status', e.target.value)}>
          <option value="AVAILABLE">Disponible</option><option value="OCCUPIED">Ocupada</option><option value="RESERVED">Reservada</option><option value="MAINTENANCE">Mantenimiento</option><option value="OUT_OF_SERVICE">Fuera de servicio</option>
        </select></label>
        <label className="full">Servicios<input value={data.services} onChange={e => set('services', e.target.value)} placeholder="Wi-Fi · TV · Desayuno"/></label>
      </div>
      <div className="checks" style={{flexDirection: 'row', gap: 20, marginBottom: 20}}>
        <label><input type="checkbox" checked={data.balcony} onChange={e => set('balcony', e.target.checked)}/>Tiene balcón</label>
        <label><input type="checkbox" checked={data.petFriendly} onChange={e => set('petFriendly', e.target.checked)}/>Admite mascotas</label>
      </div>
      {error && <p className="form-error" role="alert">{error}</p>}
      <button className="primary wide" disabled={busy}>{busy ? 'Guardando…' : isNew ? 'Crear habitación' : 'Guardar cambios'}</button>
    </form>
  </div>;
}

function AdminReservations({notify}) {
  const [items, setItems] = useState([]);
  const [filters, setFilters] = useState({query: '', status: 'ALL', from: '', to: ''});
  const load = () => api('/reservations').then(setItems);
  useEffect(() => {load()}, []);
  const update = async (r, status) => {
    await api(`/reservations/${r.id}`, {method: 'PUT', body: JSON.stringify({status})});
    notify('Estado de reserva actualizado'); load();
  };
  const filtered = useMemo(() => items.filter(r => {
    const query = filters.query.trim().toLowerCase();
    const matchesQuery = !query || r.guestName.toLowerCase().includes(query) || r.roomNumber.toLowerCase().includes(query) || String(r.id).includes(query);
    const matchesStatus = filters.status === 'ALL' || r.status === filters.status;
    const matchesFrom = !filters.from || r.checkIn >= filters.from;
    const matchesTo = !filters.to || r.checkOut <= filters.to;
    return matchesQuery && matchesStatus && matchesFrom && matchesTo;
  }), [items, filters]);

  return <>
    <div className="section-head"><div><h3>Reservas de huéspedes</h3><p className="muted">Consulta y administra el ciclo de cada estadía.</p></div></div>
    <div className="reservation-filters" aria-label="Filtros de reservas">
      <label>Huésped, habitación o número
        <input value={filters.query} onChange={e => setFilters({...filters, query: e.target.value})} placeholder="Buscar reserva"/>
      </label>
      <label>Estado
        <select value={filters.status} onChange={e => setFilters({...filters, status: e.target.value})}>
          <option value="ALL">Todos</option><option value="CONFIRMED">Confirmada</option><option value="CHECKED_IN">Alojado</option><option value="COMPLETED">Finalizada</option><option value="CANCELLED">Cancelada</option>
        </select>
      </label>
      <label>Ingreso desde<input type="date" value={filters.from} onChange={e => setFilters({...filters, from: e.target.value})}/></label>
      <label>Salida hasta<input type="date" value={filters.to} onChange={e => setFilters({...filters, to: e.target.value})}/></label>
      <button className="ghost" onClick={() => setFilters({query: '', status: 'ALL', from: '', to: ''})}>Limpiar</button>
      <span className="filter-count">{filtered.length} de {items.length}</span>
    </div>
    <div className="table-wrap"><table>
      <thead><tr><th>Reserva</th><th>Huésped</th><th>Habitación</th><th>Fechas</th><th>Total</th><th>Estado</th></tr></thead>
      <tbody>{filtered.map(r => <tr key={r.id}>
        <td><strong>#{String(r.id).padStart(4, '0')}</strong></td>
        <td>{r.guestName}</td>
        <td>{r.roomNumber} · {r.roomType}</td>
        <td>{fmtDate(r.checkIn)} — {fmtDate(r.checkOut)}</td>
        <td>{money(r.total)}</td>
        <td><select className="status-select" value={r.status} onChange={e => update(r, e.target.value)}>
          <option value="CONFIRMED">Confirmada</option><option value="CHECKED_IN">Alojado</option>
          <option value="COMPLETED">Finalizada</option><option value="CANCELLED">Cancelada</option>
        </select></td>
      </tr>)}</tbody>
    </table>
    {!filtered.length && <div className="empty small"><CalendarDays/><p>No hay reservas que coincidan con los filtros.</p></div>}
    </div>
  </>;
}

function AdminUsers() {
  const [users, setUsers] = useState([]);
  useEffect(() => {api('/users').then(setUsers)}, []);
  return <>
    <div className="section-head"><div><h3>Equipo y usuarios</h3><p className="muted">Cuentas registradas y su rol en el sistema.</p></div></div>
    <div className="user-grid">{users.map(u => <article key={u.id}>
      <div className="avatar large">{initials(u.name)}</div>
      <div><h3>{u.name}</h3><p>{u.email}</p><small>{u.phone}</small></div>
      <span className={`role-tag ${u.role.toLowerCase()}`}>{label[u.role]}</span>
    </article>)}</div>
  </>;
}

function AdminTasks({notify}) {
  const [tasks, setTasks] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [show, setShow] = useState(false);
  const [photoTask, setPhotoTask] = useState(null);
  const [form, setForm] = useState({employeeId: '', title: '', description: '', dueDate: today(), priority: 'Media', status: 'PENDING'});
  const load = () => api('/tasks').then(setTasks);
  useEffect(() => {
    load();
    api('/users').then(x => {
      const e = x.filter(u => u.role === 'EMPLOYEE');
      setEmployees(e);
      if (e[0]) setForm(f => ({...f, employeeId: e[0].id}));
    });
  }, []);
  const create = async e => {
    e.preventDefault();
    await api('/tasks', {method: 'POST', body: JSON.stringify(form)});
    setShow(false); notify('Nueva tarea asignada al empleado'); load();
  };
  const name = id => employees.find(e => e.id === id)?.name || 'Empleado';

  return <>
    <div className="section-head">
      <div><h3>Tareas del personal</h3><p className="muted">Asigna actividades y sigue su progreso.</p></div>
      <button className="primary" onClick={() => setShow(true)}><Plus size={17}/>Asignar tarea</button>
    </div>
    <div className="list-stack">{tasks.map(t => <article className="admin-task-row" key={t.id}>
      <span className={`priority ${t.priority.toLowerCase()}`}>{t.priority}</span>
      <div><h3>{t.title}</h3><p>{t.description}</p></div>
      <div><small>Responsable</small><strong>{name(t.employeeId)}</strong></div>
      <div><small>Entrega</small><strong>{fmtDate(t.dueDate)}</strong></div>
      <span className={`status ${t.status.toLowerCase()}`}>{label[t.status]}</span>
      {t.hasCompletionPhoto && <button className="evidence-button" onClick={() => setPhotoTask(t)}><Eye size={16}/>Ver evidencia</button>}
    </article>)}</div>

    {show && <div className="modal-backdrop" onMouseDown={e => e.target === e.currentTarget && setShow(false)}>
      <form className="modal" onSubmit={create}>
        <button type="button" className="modal-close" onClick={() => setShow(false)}><X/></button>
        <span className="eyebrow green">NUEVA TAREA</span>
        <h2>Asignar tarea</h2>
        <label>Título<input value={form.title} onChange={e => setForm({...form, title: e.target.value})} required/></label>
        <label>Descripción<textarea value={form.description} onChange={e => setForm({...form, description: e.target.value})} required/></label>
        <div className="form-grid">
          <label>Responsable<select value={form.employeeId} onChange={e => setForm({...form, employeeId: Number(e.target.value)})}>
            {employees.map(e => <option key={e.id} value={e.id}>{e.name}</option>)}
          </select></label>
          <label>Fecha límite<input type="date" value={form.dueDate} onChange={e => setForm({...form, dueDate: e.target.value})}/></label>
          <label className="full">Prioridad<select value={form.priority} onChange={e => setForm({...form, priority: e.target.value})}>
            <option>Alta</option><option>Media</option><option>Baja</option>
          </select></label>
        </div>
        <button className="primary wide">Asignar y notificar</button>
      </form>
    </div>}
    {photoTask && <EvidenceModal task={photoTask} onClose={() => setPhotoTask(null)}/>}
  </>;
}

function EvidenceModal({task, onClose}) {
  const [url, setUrl] = useState('');
  const [error, setError] = useState('');
  useEffect(() => {
    let objectUrl = '';
    fetch(`/api/tasks/${task.id}/photo`, {headers: {Authorization: `Bearer ${localStorage.getItem('lindomar-token') || ''}`}})
      .then(response => {if (!response.ok) throw new Error('No fue posible cargar la fotografía'); return response.blob()})
      .then(blob => {objectUrl = URL.createObjectURL(blob); setUrl(objectUrl)})
      .catch(err => setError(err.message));
    return () => {if (objectUrl) URL.revokeObjectURL(objectUrl)};
  }, [task.id]);
  return <div className="modal-backdrop" onMouseDown={e => e.target === e.currentTarget && onClose()}>
    <div className="modal evidence-modal">
      <button type="button" className="modal-close" onClick={onClose}><X/></button>
      <span className="eyebrow green">EVIDENCIA DE FINALIZACIÓN</span>
      <h2>{task.title}</h2>
      <p className="muted">{task.completionPhotoName}{task.completedAt ? ` · ${new Date(task.completedAt).toLocaleString('es-CO')}` : ''}</p>
      {error && <p className="form-error">{error}</p>}
      {url ? <img src={url} alt={`Evidencia de la tarea ${task.title}`}/> : !error && <div className="loader">Cargando fotografía…</div>}
    </div>
  </div>;
}

function Finance({notify}) {
  const [entries, setEntries] = useState([]);
  const [reminders, setReminders] = useState([]);
  const [show, setShow] = useState(false);
  const [showReminder, setShowReminder] = useState(false);
  const [form, setForm] = useState({type: 'INCOME', concept: '', amount: '', date: today()});
  const [reminderForm, setReminderForm] = useState({concept: '', category: 'Insumos', estimatedAmount: '', dueDate: future(7)});

  const load = () => {api('/finance').then(setEntries); api('/reminders').then(setReminders)};
  useEffect(() => {load()}, []);

  const create = async e => {
    e.preventDefault();
    await api('/finance', {method: 'POST', body: JSON.stringify({...form, amount: Number(form.amount)})});
    setShow(false); notify('Movimiento financiero registrado'); load();
  };
  const createReminder = async e => {
    e.preventDefault();
    await api('/reminders', {method: 'POST', body: JSON.stringify({...reminderForm, estimatedAmount: Number(reminderForm.estimatedAmount)})});
    setShowReminder(false); notify('Recordatorio programado'); load();
  };
  const pay = async r => {
    await api(`/reminders/${r.id}/pay`, {method: 'POST'});
    notify('Gasto registrado y descontado del balance'); load();
  };

  const income = entries.filter(e => e.type === 'INCOME').reduce((a, e) => a + Number(e.amount), 0);
  const expense = entries.filter(e => e.type === 'EXPENSE').reduce((a, e) => a + Number(e.amount), 0);
  const pending = reminders.filter(r => r.status === 'PENDING');

  return <>
    <div className="section-head">
      <div><h3>Control financiero</h3><p className="muted">Ingresos, gastos y balance del hotel.</p></div>
      <button className="primary" onClick={() => setShow(true)}><Plus size={17}/>Nuevo movimiento</button>
    </div>
    <div className="metric-grid three">
      <Metric icon={TrendingUp} label="Ingresos" value={money(income)} help="Reservas y otros ingresos"/>
      <Metric icon={TrendingDown} label="Gastos" value={money(expense)} help="Operación y mantenimiento"/>
      <Metric icon={WalletCards} label="Balance" value={money(income - expense)} help="Resultado acumulado"/>
    </div>

    <div className="section-head">
      <div><h3>Gastos programados</h3><p className="muted">Recordatorios de pagos y compras necesarias.</p></div>
      <button className="secondary" onClick={() => setShowReminder(true)}><Plus size={17}/>Programar gasto</button>
    </div>
    <div className="reminder-list">
      {pending.map(r => {
        const days = daysUntil(r.dueDate);
        return <div className={`reminder ${days <= 7 ? 'urgent' : ''}`} key={r.id}>
          <span>{days <= 7 ? <BellRing size={18}/> : <CalendarClock size={18}/>}</span>
          <div>
            <strong>{r.concept}</strong>
            <small>{r.category} · {money(r.estimatedAmount)} · vence {fmtDate(r.dueDate)}</small>
          </div>
          <button className="secondary" onClick={() => pay(r)}>Registrar gasto</button>
        </div>;
      })}
      {!pending.length && <div className="empty small"><CheckCircle2/><p>No hay gastos programados pendientes.</p></div>}
    </div>

    <div className="section-head"><div><h3>Movimientos registrados</h3></div></div>
    <div className="table-wrap"><table>
      <thead><tr><th>Fecha</th><th>Concepto</th><th>Tipo</th><th>Valor</th></tr></thead>
      <tbody>{[...entries].sort((a, b) => b.date.localeCompare(a.date)).map(e => <tr key={e.id}>
        <td>{fmtDate(e.date)}</td>
        <td><strong>{e.concept}</strong></td>
        <td><span className={`status ${e.type.toLowerCase()}`}>{label[e.type]}</span></td>
        <td className={e.type === 'INCOME' ? 'positive' : 'negative'}>{e.type === 'INCOME' ? '+' : '−'} {money(e.amount)}</td>
      </tr>)}</tbody>
    </table></div>

    {show && <div className="modal-backdrop" onMouseDown={e => e.target === e.currentTarget && setShow(false)}>
      <form className="modal" onSubmit={create}>
        <button type="button" className="modal-close" onClick={() => setShow(false)}><X/></button>
        <span className="eyebrow green">MOVIMIENTO</span>
        <h2>Registrar movimiento</h2>
        <label>Tipo<select value={form.type} onChange={e => setForm({...form, type: e.target.value})}>
          <option value="INCOME">Ingreso</option><option value="EXPENSE">Gasto</option>
        </select></label>
        <label>Concepto<input value={form.concept} onChange={e => setForm({...form, concept: e.target.value})} required/></label>
        <div className="form-grid">
          <label>Valor<input type="number" min="1" value={form.amount} onChange={e => setForm({...form, amount: e.target.value})} required/></label>
          <label>Fecha<input type="date" value={form.date} onChange={e => setForm({...form, date: e.target.value})}/></label>
        </div>
        <button className="primary wide">Guardar movimiento</button>
      </form>
    </div>}

    {showReminder && <div className="modal-backdrop" onMouseDown={e => e.target === e.currentTarget && setShowReminder(false)}>
      <form className="modal" onSubmit={createReminder}>
        <button type="button" className="modal-close" onClick={() => setShowReminder(false)}><X/></button>
        <span className="eyebrow green">RECORDATORIO</span>
        <h2>Programar gasto</h2>
        <p className="muted">El sistema te avisará cuando la fecha se acerque.</p>
        <label>Concepto<input value={reminderForm.concept} onChange={e => setReminderForm({...reminderForm, concept: e.target.value})} placeholder="Ej: Pago factura de agua" required/></label>
        <div className="form-grid">
          <label>Categoría<select value={reminderForm.category} onChange={e => setReminderForm({...reminderForm, category: e.target.value})}>
            <option>Insumos</option><option>Servicios</option><option>Mantenimiento</option><option>Nómina</option><option>Otro</option>
          </select></label>
          <label>Valor estimado<input type="number" min="1" value={reminderForm.estimatedAmount} onChange={e => setReminderForm({...reminderForm, estimatedAmount: e.target.value})} required/></label>
          <label className="full">Fecha límite<input type="date" min={today()} value={reminderForm.dueDate} onChange={e => setReminderForm({...reminderForm, dueDate: e.target.value})}/></label>
        </div>
        <button className="primary wide">Programar recordatorio</button>
      </form>
    </div>}
  </>;
}

/* ---------- Barrera de errores ---------- */
/**
 * Evita que un error de renderizado deje la pantalla completamente en blanco:
 * muestra un mensaje entendible y permite volver sin perder la sesión.
 */
class ErrorBoundary extends React.Component {
  constructor(props) {super(props); this.state = {error: null}}
  static getDerivedStateFromError(error) {return {error}}
  componentDidCatch(error, info) {console.error('Error de renderizado:', error, info)}
  render() {
    if (!this.state.error) return this.props.children;
    return <div className="crash-screen">
      <span className="brand-mark">L</span>
      <h1>Algo no cargó bien</h1>
      <p>Ocurrió un problema al mostrar esta sección. Puedes volver e intentarlo de nuevo.</p>
      <div className="crash-actions">
        <button className="primary" onClick={() => this.setState({error: null})}>Reintentar</button>
        <button className="ghost" onClick={() => location.reload()}>Recargar la página</button>
      </div>
      <code>{String(this.state.error?.message || this.state.error)}</code>
    </div>;
  }
}

/* ---------- Raíz ---------- */
function App() {
  const [user, setUser] = useState(undefined);
  // Fuente única del tema: aplica también a la pantalla de inicio de sesión.
  const [theme, toggleTheme] = useTheme();
  useEffect(() => {
    localStorage.removeItem('lindomar-user');
    if (!localStorage.getItem('lindomar-token')) {setUser(null); return}
    api('/auth/me').then(setUser).catch(() => {localStorage.removeItem('lindomar-token'); setUser(null)});
  }, []);
  const login = session => {localStorage.setItem('lindomar-token', session.token); setUser(session.user)};
  const logout = () => {api('/auth/logout', {method: 'POST'}).catch(() => {}).finally(() => {localStorage.removeItem('lindomar-token'); setUser(null)})};
  return <ErrorBoundary>
    {user === undefined ? <div className="loader">Validando sesión…</div> : user
      ? <Shell user={user} onLogout={logout} theme={theme} toggleTheme={toggleTheme}/>
      : <Login onLogin={login}/>}
  </ErrorBoundary>;
}

createRoot(document.getElementById('root')).render(<App/>);
