# Alcance del primer avance

## Objetivo

Integrar selección pública de horarios con atención humana por WhatsApp y una reserva confirmada persistida en PostgreSQL. El panel interno es la fuente de disponibilidad.

## Recorrido para la presentación

1. Abrir la web y mostrar sedes, modalidades y tarifas.
2. Seleccionar una fecha futura y solicitar **Cancha A, fútbol 7, 18–19**.
3. Registrar otra solicitud para **Mitad A+B, fútbol 9, 18–19** en la misma sede y fecha.
4. Abrir el panel, localizar ambas solicitudes y mostrar la coincidencia.
5. Marcar una pendiente de pago. Mostrar que la web sigue ofreciendo el horario.
6. Registrar el pago completo y confirmar la primera.
7. Mostrar el cierre automático de la segunda, el historial y la ocupación de A, AB y cancha completa; C/D siguen disponibles.
8. Cambiar la segunda a otro horario y reabrirla, conservando cliente e historial.

La apertura de enlaces WhatsApp no envía mensajes automáticamente. La presentación puede mostrar el enlace y el texto sin contactar a la empresa.

## Implementado

Catálogo persistente, disponibilidad por sectores, precios semanales/especiales, clientes por celular, solicitudes web/internas, inicio de sesión, permisos por sede, bandeja, notas, edición, reapertura, verificación manual de pago completo, confirmación transaccional, cierre de competidores e historial.

Los datos iniciales se mantienen en PostgreSQL y las migraciones; el catálogo y las tarifas pueden actualizarse. No se han validado todas las reglas comerciales con la operación privada de la empresa.

## Próximas iteraciones acordadas

- Pantallas de configuración de sedes, canchas, sectores, tarifas y números WhatsApp.
- Jerarquía completa de horarios: sede, cancha y excepciones por fecha; hoy solo horario semanal de sede.
- Roles y permisos administrables, acceso gerencial y fusión de clientes duplicados.
- Cotizaciones, revisiones, servicios especiales y conversión a evento.
- Bloqueos tentativos decididos por el operador, con vencimiento obligatorio.
- Bloqueos normales con resolución previa de reservas afectadas.
- Bloqueos de emergencia con permiso especial, incidencias y auditoría.
- Series recurrentes confirmadas por acuerdo comercial, con cronograma de pagos.
- Cancelaciones y reprogramación de reservas confirmadas según anticipación; excepciones autorizadas.
- Reembolsos pendientes/realizados y trazabilidad financiera.
- Reportes por sede, ingresos, ocupación y demanda.
- Mayor fidelidad visual con la web original y cobertura de accesibilidad.
- Despliegue en Oracle Cloud, HTTPS, copias de seguridad y controles de acceso público.

## Límites actuales

- Una reserva confirmada no puede editarse o cancelarse en esta interfaz; se implementará junto con la política y reembolsos.
- El panel lista hasta 300 solicitudes, sin paginación de servidor todavía. Los contadores resumen esa lista, no son reportes contables.
- La ficha de cliente se recupera por celular; no se actualiza automáticamente con datos públicos diferentes para evitar sobrescribirla.
- El catálogo y las tarifas no tienen editor visual todavía.
- Estado `UNAVAILABLE` conserva a los clientes a quienes el operador debe avisar; no hay seguimiento separado de mensajes enviados.
- Atención directa registra origen `WHATSAPP`; la selección de otros orígenes queda pendiente.
- No hay API de WhatsApp, envío automático, pasarela de pago ni verificación bancaria.

## Criterios de aceptación del núcleo

1. Solicitud persistida antes de abrir WhatsApp.
2. Pendiente de pago no reduce disponibilidad.
3. Confirmación solo con pago completo verificado por operador autorizado.
4. Nunca dos confirmaciones por sectores y horarios superpuestos a través de la API.
5. Confirmar cierra solicitudes incompatibles, no las de sectores independientes.
6. Reapertura conserva el código, el cliente y el historial.
7. Precio y disponibilidad se validan en el servidor.
