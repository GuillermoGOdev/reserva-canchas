import { useEffect, useState } from 'react';
import { ArrowLeft, ArrowRight, Check, CheckCircle2, ChevronLeft, ChevronRight, Clock3, MapPin, MessageCircle, ShieldCheck, X } from 'lucide-react';
import { api, post } from './api';
import { dateLabel, hour, money, today } from './types';
import type { Availability, Catalog, Slot } from './types';

export default function Booking({catalog,onCreated,allowedSites}:{catalog:Catalog|null;onCreated?:()=>void;allowedSites?:number[]}) {
 const initial=Number(new URLSearchParams(location.hash.split('?')[1]).get('sede'))||allowedSites?.[0]||1;
 const [site,setSite]=useState(initial), [modality,setModality]=useState(7),[date,setDate]=useState(today());
 const [data,setData]=useState<Availability|null>(null),[selected,setSelected]=useState<Slot[]>([]),[loading,setLoading]=useState(false),[error,setError]=useState('');
 const [form,setForm]=useState(false),[busy,setBusy]=useState(false),[name,setName]=useState(''),[phone,setPhone]=useState(''),[email,setEmail]=useState('');
 const [success,setSuccess]=useState<{code:string;url:string}|null>(null);
 const siteInfo=catalog?.sites.find(s=>s.id===site);
 const modalities=[...new Set(catalog?.spaces.filter(s=>s.site_id===site).map(s=>s.modality))].sort((a,b)=>a-b);
 const total=selected.reduce((sum,s)=>sum+s.price,0);
 const selectedSpace=catalog?.spaces.find(s=>s.id===selected[0]?.spaceId);
 useEffect(()=>{
  let active=true;
  setSelected([]);setLoading(true);setError('');setData(null);
  async function load(){try{const d=await api<Availability>(`/public/availability?siteId=${site}&date=${date}&modality=${modality}`);if(active){setData(d);setLoading(false)}}catch(e){if(active){setError((e as Error).message);setLoading(false)}}}
  load();const timer=window.setInterval(load,15000);return()=>{active=false;clearInterval(timer)};
 },[site,date,modality]);
 useEffect(()=>{if(data)setSelected(previous=>previous.filter(s=>data.slots.some(x=>x.spaceId===s.spaceId && x.hour===s.hour && x.available)).map(s=>data.slots.find(x=>x.spaceId===s.spaceId&&x.hour===s.hour)!))},[data]);
 function select(slot:Slot){
  if(!slot.available)return;
  setError('');
  if(!selected.length || selected[0].spaceId!==slot.spaceId){setSelected([slot]);return}
  if(selected.some(s=>s.hour===slot.hour)){setSelected(selected.length===1?[]:selected.filter(s=>s.hour<slot.hour));return}
  const min=Math.min(slot.hour,...selected.map(s=>s.hour)),max=Math.max(slot.hour,...selected.map(s=>s.hour));
  const range=data!.slots.filter(s=>s.spaceId===slot.spaceId && s.hour>=min && s.hour<=max);
  if(range.some(s=>!s.available)){setSelected([slot]);return}
  setSelected(range.sort((a,b)=>a.hour-b.hour));
 }
 function moveDate(amount:number){const d=new Date(date+'T12:00:00');d.setDate(d.getDate()+amount);const next=d.toISOString().slice(0,10);if(next>=today())setDate(next)}
 async function submit(e:React.FormEvent){
  e.preventDefault();if(!selected.length)return;setBusy(true);setError('');
  try{
   const start=Math.min(...selected.map(s=>s.hour)),end=Math.max(...selected.map(s=>s.hour))+1;
   const result=await post<{code:string;total:number;site:{name:string;space_name:string;whatsapp:string}}>(onCreated?'/requests':'/public/requests',{name,phone,email,spaceId:selected[0].spaceId,date,startHour:start,endHour:end});
   const message=`Hola Sport School, mi solicitud es ${result.code}. Soy ${name}. Quisiera reservar en ${result.site.name}, ${result.site.space_name} (fútbol ${modality}), el ${date}, de ${hour(start)} a ${hour(end)}. Importe: ${money(result.total)}. Quisiera coordinar la disponibilidad y el pago.`;
   const url=`https://wa.me/${result.site.whatsapp}?text=${encodeURIComponent(message)}`;
   setSuccess({code:result.code,url});setForm(false);
   if(onCreated)onCreated();else window.open(url,'_blank','noopener,noreferrer');
  }catch(e){setError((e as Error).message)}finally{setBusy(false)}
 }
 return <main className={'booking-page '+(onCreated?'embedded-booking':'')}>
  {!onCreated&&<a className="back-link" href="#inicio"><ArrowLeft size={16}/> Volver al inicio</a>}
  <div className="booking-title"><div><span className="eyebrow green">{onCreated?'ATENCIÓN DIRECTA':'NOS VEMOS EN LA CANCHA'}</span><h1>{onCreated?'Nueva solicitud':'Encuentra tu próximo partido.'}</h1><p>Elige tu espacio y horario. {onCreated?'Registra los datos del cliente.':'Coordinamos los detalles contigo por WhatsApp.'}</p></div><div className="step-indicator"><span className="active">1 <small>Elige</small></span><i/><span>2 <small>Solicita</small></span><i/><span>3 <small>Juega</small></span></div></div>
  <div className="booking-layout"><div className="booking-main">
   <div className="booking-filters"><label>Sede<select value={site} onChange={e=>{setSite(+e.target.value);setModality(7)}}>{catalog?.sites.filter(s=>!allowedSites||allowedSites.includes(s.id)).map(s=><option key={s.id} value={s.id}>{s.name}</option>)}</select></label><div><label>Modalidad</label><div className="modality-tabs">{modalities.map(m=><button className={m===modality?'selected':''} onClick={()=>setModality(m)} key={m}>Fútbol {m}</button>)}</div></div></div>
   <div className="schedule-card"><div className="schedule-toolbar"><div className="date-control"><button aria-label="Día anterior" onClick={()=>moveDate(-1)} disabled={date===today()}><ChevronLeft size={18}/></button><label><input aria-label="Fecha de reserva" type="date" value={date} min={today()} onChange={e=>e.target.value&&setDate(e.target.value)}/></label><button aria-label="Día siguiente" onClick={()=>moveDate(1)}><ChevronRight size={18}/></button></div><div className="legend"><span><i className="dot-free"/>Disponible</span><span><i className="dot-selected"/>Seleccionado</span><span><i className="dot-busy"/>Ocupado</span></div></div>
   {loading?<div className="empty-state"><div className="spinner"/>Consultando disponibilidad…</div>:data&&<div className="schedule-scroll"><table className="schedule-table"><thead><tr><th><Clock3 size={16}/> Hora</th>{data.spaces.map(s=><th key={s.id}>{s.name}<small>Fútbol {s.modality}</small></th>)}</tr></thead><tbody>{Array.from({length:data.closeHour-data.openHour},(_,i)=>i+data.openHour).map(h=><tr key={h}><td>{hour(h)}<small>{hour(h+1)}</small></td>{data.spaces.map(space=>{const slot=data.slots.find(s=>s.spaceId===space.id&&s.hour===h)!;const active=selected.some(s=>s.spaceId===space.id&&s.hour===h);return <td key={space.id}><button disabled={!slot.available} onClick={()=>select(slot)} className={'slot '+(active?'slot-selected':'')} aria-pressed={active} aria-label={`${space.name}, ${hour(h)}, ${slot.available?money(slot.price):'no disponible'}`}>{slot.available?<>{active&&<Check size={14}/>} {money(slot.price)}</>:<span>—</span>}</button></td>})}</tr>)}</tbody></table></div>}
   <div className="schedule-foot"><ShieldCheck size={15}/> Disponibilidad actualizada cada 15 segundos. Precios por hora.</div></div>
   {error&&!form&&<p role="alert" className="error">{error}</p>}
  </div><aside className="booking-summary"><img src={siteInfo?.image} alt={siteInfo?.name}/><div className="summary-body"><span className="eyebrow green">TU PARTIDO</span><h2>{siteInfo?.name}</h2><p className="muted flex items-center gap-1"><MapPin size={14}/>{siteInfo?.address}</p><hr/>{selected.length?<><dl><div><dt>Espacio</dt><dd>{selectedSpace?.name} · F{modality}</dd></div><div><dt>Fecha</dt><dd>{dateLabel(date)}</dd></div><div><dt>Horario</dt><dd>{hour(Math.min(...selected.map(s=>s.hour)))} – {hour(Math.max(...selected.map(s=>s.hour))+1)}</dd></div><div><dt>Duración</dt><dd>{selected.length} {selected.length===1?'hora':'horas'}</dd></div></dl><div className="total"><span>Total</span><strong>{money(total)}</strong></div></>:<div className="selection-placeholder"><Clock3 size={28}/><p>Selecciona uno o más bloques consecutivos de una cancha.</p></div>}<button className="btn btn-dark w-full" disabled={!selected.length} onClick={()=>{setForm(true);setError('')}}>{onCreated?'Registrar solicitud':'Continuar'}<ArrowRight size={17}/></button><p className="summary-note">La solicitud no separa el horario. Tu reserva se confirma cuando el personal verifica el pago completo.</p></div></aside></div>
  {form&&<div className="modal-backdrop" onClick={()=>!busy&&setForm(false)}><section className="modal" role="dialog" aria-modal="true" aria-labelledby="request-title" onClick={e=>e.stopPropagation()}><button className="modal-close" aria-label="Cerrar" onClick={()=>setForm(false)} disabled={busy}><X/></button><span className="eyebrow green">UN PASO MÁS</span><h2 id="request-title">{onCreated?'Datos del cliente':'¿Con quién coordinamos?'}</h2><p className="muted">{selectedSpace?.name} · {dateLabel(date)} · {money(total)}</p><form onSubmit={submit}><label>Nombre completo<input autoFocus required maxLength={120} value={name} onChange={e=>setName(e.target.value)} placeholder="Tu nombre y apellido" autoComplete="name"/></label><label>Celular<div className="phone-input"><span>+51</span><input required pattern="9[0-9]{8}" maxLength={9} value={phone} onChange={e=>setPhone(e.target.value.replace(/\D/g,''))} placeholder="999 999 999" autoComplete="tel-national" inputMode="tel"/></div></label><label>Correo electrónico <small>(opcional)</small><input type="email" maxLength={180} value={email} onChange={e=>setEmail(e.target.value)} placeholder="nombre@correo.com" autoComplete="email"/></label>{error&&<div className="error" role="alert">{error}</div>}<div className="info-box"><MessageCircle size={20}/><p>Guardaremos tu solicitud. {onCreated?'Continúa la atención desde la bandeja.':'Después envía el mensaje por WhatsApp para conversar con nuestro equipo antes de pagar.'}</p></div><button className="btn btn-dark w-full" disabled={busy||!selected.length}>{busy?'Registrando…':onCreated?'Guardar solicitud':'Solicitar por WhatsApp'}<ArrowUpIcon/></button></form></section></div>}
  {success&&!onCreated&&<div className="modal-backdrop"><section className="modal text-center" role="dialog" aria-modal="true" aria-label="Solicitud registrada"><div className="success-icon"><CheckCircle2 size={38}/></div><h2>¡Solicitud registrada!</h2><p className="muted">Tu código de atención</p><div className="request-code">{success.code}</div><p>Envía el mensaje por WhatsApp para coordinar tu reserva. El horario sigue disponible hasta verificar el pago.</p><a className="btn btn-dark w-full" href={success.url} target="_blank" rel="noreferrer"><MessageCircle size={18}/> Continuar en WhatsApp</a><button className="btn btn-outline w-full mt-3" onClick={()=>{setSuccess(null);setSelected([])}}>Volver a los horarios</button></section></div>}
 </main>
}
function ArrowUpIcon(){return <ArrowRight size={17}/>}
