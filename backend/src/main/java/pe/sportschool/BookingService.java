package pe.sportschool;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookingService {
    private final JdbcTemplate db;
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    public BookingService(JdbcTemplate db) { this.db = db; }
    static void require(boolean ok, String message) { if (!ok) throw new ResponseStatusException(HttpStatus.CONFLICT, message); }
    private void lock() { db.execute("SELECT pg_advisory_xact_lock(736001)"); }
    Map<String,Object> one(String sql, Object... params) {
        var rows = db.queryForList(sql, params);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro no encontrado");
        return rows.getFirst();
    }
    public Map<String,Object> user(String username) { return one("SELECT id,username,name,role FROM app_users WHERE username=? AND active=true", username); }
    public List<Map<String,Object>> sitesFor(String username) {
        return db.queryForList("SELECT s.* FROM sites s JOIN user_sites us ON us.site_id=s.id JOIN app_users u ON u.id=us.user_id WHERE u.username=? ORDER BY s.id", username);
    }
    void checkAccess(String username, long spaceId) {
        var count = db.queryForObject("SELECT count(*) FROM spaces s JOIN physical_fields f ON f.id=s.field_id JOIN user_sites us ON us.site_id=f.site_id JOIN app_users u ON u.id=us.user_id WHERE s.id=? AND u.username=? AND u.active=true", Integer.class, spaceId, username);
        if (count == null || count == 0) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta sede");
    }
    public Map<String,Object> catalog() {
        return Map.of("sites", db.queryForList("SELECT * FROM sites ORDER BY id"), "spaces", db.queryForList("SELECT s.*,f.site_id, string_agg(sec.name, ', ' ORDER BY sec.name) AS sectors FROM spaces s JOIN physical_fields f ON f.id=s.field_id JOIN space_sectors ss ON ss.space_id=s.id JOIN sectors sec ON sec.id=ss.sector_id WHERE s.active=true GROUP BY s.id,f.site_id ORDER BY s.id"));
    }
    private BigDecimal rate(long space, LocalDate date, int hour) {
        var special = db.queryForList("SELECT amount FROM special_rates WHERE space_id=? AND date=? AND hour=?", space, date, hour);
        if (!special.isEmpty()) return (BigDecimal) special.getFirst().get("amount");
        var normal = db.queryForList("SELECT amount FROM rates WHERE space_id=? AND day_of_week=? AND start_hour<=? AND end_hour>? ORDER BY id DESC LIMIT 1", space,date.getDayOfWeek().getValue(),hour,hour);
        return normal.isEmpty() ? null : (BigDecimal) normal.getFirst().get("amount");
    }
    private boolean overlaps(long space, LocalDate date, int start, int end) {
        return db.queryForObject("SELECT EXISTS(SELECT 1 FROM reservations r WHERE r.date=? AND r.start_hour<? AND r.end_hour>? AND EXISTS(SELECT 1 FROM space_sectors a JOIN space_sectors b ON a.sector_id=b.sector_id WHERE a.space_id=r.space_id AND b.space_id=?))", Boolean.class, date,end,start,space);
    }
    private Map<String,Object> hours(long site, LocalDate date) {
        return one("SELECT * FROM operating_hours WHERE site_id=? AND day_of_week=?", site,date.getDayOfWeek().getValue());
    }
    public Map<String,Object> availability(long site, LocalDate date, Integer modality) {
        var hours = hours(site,date);
        int open = ((Number) hours.get("open_hour")).intValue(), close = ((Number) hours.get("close_hour")).intValue();
        var spaces = db.queryForList("SELECT s.* FROM spaces s JOIN physical_fields f ON f.id=s.field_id WHERE f.site_id=? AND s.active=true AND (?=0 OR s.modality=?) ORDER BY s.id", site,modality == null ? 0 : modality,modality == null ? 0 : modality);
        var slots = new ArrayList<Map<String,Object>>();
        var now = LocalDateTime.now(LIMA);
        for (var space : spaces) {
            long id = ((Number) space.get("id")).longValue();
            for (int h=open;h<close;h++) {
                var price = rate(id,date,h);
                boolean future = date.atTime(h,0).isAfter(now) && !date.isAfter(now.toLocalDate().plusDays(180));
                slots.add(Map.of("spaceId",id,"hour",h,"price",price == null ? BigDecimal.ZERO : price,"available",price != null && future && !overlaps(id,date,h,h+1)));
            }
        }
        return Map.of("spaces",spaces,"slots",slots,"openHour",open,"closeHour",close);
    }
    private BigDecimal validateSlot(long space, LocalDate date, int start, int end) {
        require(date != null && start>=0 && end<=24 && end>start,"Selecciona horas consecutivas válidas");
        var now = LocalDateTime.now(LIMA);
        require(date.atTime(start,0).isAfter(now),"El horario ya pasó. Selecciona otro.");
        require(!date.isAfter(now.toLocalDate().plusDays(180)),"Puedes solicitar hasta 180 días de anticipación");
        var field = one("SELECT s.*, f.site_id FROM spaces s JOIN physical_fields f ON f.id=s.field_id WHERE s.id=? AND s.active=true",space);
        var operating = hours(((Number) field.get("site_id")).longValue(),date);
        require(start>=((Number)operating.get("open_hour")).intValue() && end<=((Number)operating.get("close_hour")).intValue(),"El horario está fuera de atención");
        require(!overlaps(space,date,start,end),"El espacio ya está reservado. Selecciona otro horario.");
        BigDecimal total = BigDecimal.ZERO;
        for(int h=start;h<end;h++) { var price = rate(space,date,h); require(price != null,"No hay tarifa configurada para ese horario"); total=total.add(price); }
        return total;
    }
    private void history(long id, String actor, String action, String detail) { db.update("INSERT INTO request_history(request_id,actor,action,details) VALUES (?,?,?,?)",id,actor,action,detail); }
    @Transactional
    public Map<String,Object> create(ApiController.NewRequest form, String operator) {
        lock();
        if (operator != null) checkAccess(operator,form.spaceId());
        BigDecimal total = validateSlot(form.spaceId(),form.date(),form.startHour(),form.endHour());
        String phone = form.phone().replaceAll("\\D", "");
        if (phone.length()==11 && phone.startsWith("51")) phone=phone.substring(2);
        require(phone.matches("9\\d{8}"),"Ingresa un celular peruano de 9 dígitos");
        String email = form.email() == null || form.email().isBlank() ? null : form.email().trim();
        db.update("INSERT INTO customers(name,phone,email) VALUES (?,?,?) ON CONFLICT(phone) DO NOTHING", form.name().trim(),phone,email);
        long customer = db.queryForObject("SELECT id FROM customers WHERE phone=?", Long.class,phone);
        String code = "SS-" + UUID.randomUUID().toString().substring(0,8).toUpperCase();
        long id = db.queryForObject("INSERT INTO requests(code,customer_id,space_id,date,start_hour,end_hour,quoted_price,agreed_price,source) VALUES (?,?,?,?,?,?,?,?,?) RETURNING id", Long.class,code,customer,form.spaceId(),form.date(),form.startHour(),form.endHour(),total,total,operator == null ? "WEB" : "WHATSAPP");
        history(id,operator == null ? "Cliente · web" : operator,"Solicitud creada",form.date()+" · "+form.startHour()+":00–"+form.endHour()+":00 · S/ "+total);
        var site = one("SELECT si.name,s.name AS space_name,si.whatsapp FROM sites si JOIN physical_fields f ON f.site_id=si.id JOIN spaces s ON s.field_id=f.id WHERE s.id=?",form.spaceId());
        return Map.of("id",id,"code",code,"total",total,"site",site,"message","Solicitud registrada");
    }
    private static final String DETAILS = "SELECT q.*,c.name AS customer_name,c.phone,c.email,s.name AS space_name,s.modality,f.site_id,si.name AS site_name,si.whatsapp FROM requests q JOIN customers c ON c.id=q.customer_id JOIN spaces s ON s.id=q.space_id JOIN physical_fields f ON f.id=s.field_id JOIN sites si ON si.id=f.site_id ";
    public List<Map<String,Object>> requests(String username) {
        return db.queryForList(DETAILS + "WHERE EXISTS(SELECT 1 FROM user_sites us JOIN app_users u ON u.id=us.user_id WHERE us.site_id=f.site_id AND u.username=?) ORDER BY q.created_at DESC LIMIT 300",username);
    }
    public Map<String,Object> detail(long id,String username) {
        var row = one(DETAILS+"WHERE q.id=?",id);
        checkAccess(username,((Number)row.get("space_id")).longValue());
        var result = new LinkedHashMap<>(row);
        result.put("history",db.queryForList("SELECT * FROM request_history WHERE request_id=? ORDER BY created_at DESC,id DESC",id));
        result.put("payments",db.queryForList("SELECT p.* FROM payments p JOIN reservations r ON r.id=p.reservation_id WHERE r.request_id=?",id));
        result.put("competing",db.queryForList("SELECT q.id,q.code,c.name,q.status FROM requests q JOIN customers c ON c.id=q.customer_id WHERE q.id<>? AND q.date=? AND q.start_hour<? AND q.end_hour>? AND q.status IN ('NEW','CONTACTED','AWAITING_PAYMENT') AND EXISTS(SELECT 1 FROM space_sectors a JOIN space_sectors b ON a.sector_id=b.sector_id WHERE a.space_id=q.space_id AND b.space_id=?)",id,row.get("date"),row.get("end_hour"),row.get("start_hour"),row.get("space_id")));
        result.put("customer_history",db.queryForList("SELECT q.id,q.code,q.date,q.status,q.agreed_price FROM requests q JOIN spaces s ON s.id=q.space_id JOIN physical_fields f ON f.id=s.field_id WHERE q.customer_id=? AND EXISTS(SELECT 1 FROM user_sites us JOIN app_users u ON u.id=us.user_id WHERE us.site_id=f.site_id AND u.username=?) ORDER BY q.created_at DESC LIMIT 30",row.get("customer_id"),username));
        return result;
    }
    @Transactional
    public Map<String,Object> update(long id, ApiController.UpdateRequest form, String username) {
        lock();
        var old = one("SELECT * FROM requests WHERE id=?",id);
        checkAccess(username,((Number)old.get("space_id")).longValue());
        checkAccess(username,form.spaceId());
        require(!"RESERVED".equals(old.get("status")),"La solicitud ya se convirtió en reserva");
        require(Set.of("NEW","CONTACTED","AWAITING_PAYMENT","WITHDRAWN","UNAVAILABLE").contains(form.status()),"Estado no permitido");
        boolean changed = ((Number)old.get("space_id")).longValue()!=form.spaceId() || !old.get("date").toString().equals(form.date().toString()) || ((Number)old.get("start_hour")).intValue()!=form.startHour() || ((Number)old.get("end_hour")).intValue()!=form.endHour();
        BigDecimal calculated = (BigDecimal)old.get("agreed_price");
        if(changed || Set.of("NEW","CONTACTED","AWAITING_PAYMENT").contains(form.status())) {
            var current = validateSlot(form.spaceId(),form.date(),form.startHour(),form.endHour());
            if (changed) calculated=current;
        }
        BigDecimal agreed = form.agreedPrice() == null ? calculated : form.agreedPrice();
        require(agreed.signum()>=0,"El precio no puede ser negativo");
        boolean priceChanged = agreed.compareTo(calculated)!=0;
        if(priceChanged) require(form.reason()!=null && !form.reason().isBlank(),"Indica el motivo del ajuste de precio");
        String detail = "Estado: "+old.get("status")+" → "+form.status()+". Horario: "+old.get("date")+" "+old.get("start_hour")+"–"+old.get("end_hour")+" (espacio "+old.get("space_id")+") → "+form.date()+" "+form.startHour()+"–"+form.endHour()+" (espacio "+form.spaceId()+"). Importe: S/ "+old.get("agreed_price")+" → S/ "+agreed;
        if(form.reason()!=null && !form.reason().isBlank()) detail+=". Motivo: "+form.reason();
        if (!Objects.equals(old.get("notes"),form.notes())) detail+=". Notas: "+old.get("notes")+" → "+form.notes();
        db.update("UPDATE requests SET space_id=?,date=?,start_hour=?,end_hour=?,status=?,agreed_price=?,notes=?,updated_at=now() WHERE id=?",form.spaceId(),form.date(),form.startHour(),form.endHour(),form.status(),agreed,form.notes(),id);
        history(id,username,"Solicitud actualizada",detail);
        return detail(id,username);
    }
    @Transactional
    public Map<String,Object> confirm(long id, ApiController.ConfirmRequest form, String username) {
        lock();
        var request = one("SELECT * FROM requests WHERE id=?",id);
        long space = ((Number)request.get("space_id")).longValue();
        checkAccess(username,space);
        require(Set.of("NEW","CONTACTED","AWAITING_PAYMENT").contains(request.get("status")),"La solicitud no está abierta");
        var date = LocalDate.parse(request.get("date").toString());
        int start=((Number)request.get("start_hour")).intValue(), end=((Number)request.get("end_hour")).intValue();
        validateSlot(space,date,start,end);
        var price = (BigDecimal)request.get("agreed_price");
        require(form.amount().compareTo(price)==0,"Debes verificar el importe completo acordado: S/ "+price);
        require(Set.of("YAPE","PLIN","TRANSFERENCIA","EFECTIVO","TARJETA").contains(form.method()),"Método de pago no válido");
        long userId=((Number)user(username).get("id")).longValue();
        long reservation = db.queryForObject("INSERT INTO reservations(request_id,space_id,date,start_hour,end_hour,total,confirmed_by) VALUES (?,?,?,?,?,?,?) RETURNING id",Long.class,id,space,date,start,end,price,userId);
        db.update("INSERT INTO payments(reservation_id,amount,method,reference,verified_by) VALUES (?,?,?,?,?)",reservation,price,form.method(),form.reference(),userId);
        db.update("UPDATE requests SET status='RESERVED',updated_at=now() WHERE id=?",id);
        history(id,username,"Reserva confirmada","Pago completo verificado: S/ "+price+" · "+form.method()+" · Operación: "+Objects.toString(form.reference(),""));
        var conflicts=db.queryForList("SELECT q.id FROM requests q WHERE q.id<>? AND q.date=? AND q.start_hour<? AND q.end_hour>? AND q.status IN ('NEW','CONTACTED','AWAITING_PAYMENT') AND EXISTS(SELECT 1 FROM space_sectors a JOIN space_sectors b ON a.sector_id=b.sector_id WHERE a.space_id=q.space_id AND b.space_id=?)",id,date,end,start,space);
        for(var conflict:conflicts) {
            long closed=((Number)conflict.get("id")).longValue();
            db.update("UPDATE requests SET status='UNAVAILABLE',updated_at=now() WHERE id=?",closed);
            history(closed,username,"Cierre por falta de disponibilidad","El horario se ocupó con la reserva "+request.get("code")+". Contactar al cliente para ofrecer otro horario.");
        }
        return Map.of("reservationId",reservation,"closedRequests",conflicts.size(),"message","Reserva confirmada. Disponibilidad actualizada.");
    }
}
