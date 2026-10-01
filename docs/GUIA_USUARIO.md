# Guía de uso

## Cliente: solicitar una cancha

1. Abre la web y pulsa **Reserva tu cancha** o elige una sede.
2. Selecciona sede, modalidad y fecha. Cada columna identifica un espacio concreto y cada fila una hora.
3. Pulsa un bloque disponible. Para más horas, pulsa otro bloque de la misma columna: se selecciona el intervalo consecutivo si está disponible.
4. Revisa el resumen y el total. Pulsa **Continuar**.
5. Ingresa nombre y celular peruano de nueve dígitos. El correo es opcional.
6. Pulsa **Solicitar por WhatsApp**. La solicitud queda registrada y se genera un código.
7. Envía el mensaje preparado a Sport School. Si el navegador bloquea la nueva pestaña, usa el botón **Continuar en WhatsApp** de la pantalla de éxito.
8. Conversa con el operador antes de pagar y envía el comprobante por WhatsApp.

**Registrar una solicitud no aparta la cancha.** El espacio se ocupa cuando el personal verifica el pago completo y confirma la reserva. Abrir WhatsApp tampoco garantiza que se haya enviado el mensaje.

La grilla consulta cambios cada 15 segundos; al solicitar se comprueba nuevamente la disponibilidad. No se envía ningún pago desde la web.

## Operador: atender una solicitud

1. Entra a **Acceso del personal** en el pie de página o a `/#admin`.
2. Inicia sesión. La bandeja muestra solicitudes solamente de tus sedes asignadas.
3. Busca por nombre, celular o código; filtra por sede y estado.
4. Abre una fila para ver el detalle, las otras solicitudes coincidentes y el historial.
5. Usa **Conversar por WhatsApp** para preparar el mensaje al cliente. El envío es manual desde tu sesión de WhatsApp.
6. Cambia el estado a **En conversación** o **Pendiente de pago**, agrega notas y pulsa **Guardar cambios**.

### Estados

| Estado | Significado | Ocupa cancha |
|---|---|---|
| Nueva | Registrada, falta atender | No |
| En conversación | Se están coordinando detalles | No |
| Pendiente de pago | El cliente indicó que pagará | No |
| Reserva confirmada | Pago completo verificado y espacio asignado | Sí |
| Sin disponibilidad | Se concretó otra reserva incompatible | No |
| Desistida | La solicitud no continuará | No |

### Cambiar cancha, fecha, hora o precio

- Edita los campos y guarda. Si cambias el espacio o intervalo, el servidor recalcula el total.
- Para ajustar el importe negociado, conserva el horario, ingresa el nuevo precio y explica el motivo. Puedes hacerlo después de guardar un cambio de horario.
- El precio original de la solicitud y cada modificación permanecen en el historial.
- Una solicitud cerrada puede reutilizarse: cambia a un horario disponible y selecciona **Nueva** o **En conversación**.

### Confirmar

1. Comprueba directamente que el pago se recibió; la aplicación no verifica Yape ni otros bancos.
2. Guarda cualquier cambio pendiente de la solicitud.
3. En **Verificar pago y confirmar**, elige método e ingresa el importe recibido, que debe coincidir con el importe acordado.
4. Agrega el número de operación si lo tienes.
5. Marca que verificaste el pago completo y pulsa **Confirmar reserva**.
6. El sistema registra reserva y pago, ocupa todos los sectores utilizados y cierra las solicitudes incompatibles.
7. Usa **Preparar confirmación por WhatsApp** para enviar manualmente la confirmación. Avisa también a los clientes cuyas solicitudes se cerraron.

Solo una reserva incompatible puede confirmarse, incluso si dos operadores intentan hacerlo al mismo tiempo. Los pendientes de pago nunca bloquean disponibilidad.

### Atención fuera de la web

Pulsa **Nueva solicitud**, elige el espacio y horario e ingresa los datos del cliente. Continúa desde la bandeja. El celular permite recuperar su historial existente sin registrar otra ficha para ese número.

### Reservas y clientes

- **Reservas:** lista de solicitudes convertidas en reservas, con pagos verificados.
- **Clientes:** clientes registrados en tus sedes, con acceso al detalle y al historial de sus solicitudes.
- **Historial:** acciones, usuario, momento y detalle de cambios. Los tiempos se presentan según la configuración horaria del navegador.

## Administrador

En esta entrega, comparte las funciones de atención y tiene asignadas ambas sedes. El catálogo, tarifas y asignaciones se gestionan en PostgreSQL siguiendo la guía técnica; sus pantallas de configuración pertenecen a la siguiente iteración.

## Problemas frecuentes

- **No conecta:** inicia base, backend y frontend; abre `http://127.0.0.1:5173`.
- **No quedan horarios hoy:** el sistema no permite solicitar horas que ya comenzaron; selecciona otra fecha.
- **El horario ya fue reservado:** actualiza la grilla y ofrece un horario distinto.
- **Pago incompleto:** verifica el importe total acordado; no se aceptan adelantos en alquileres particulares.
- **Error de sesión o permiso:** recarga, vuelve a iniciar sesión y comprueba la sede asignada.
- **WhatsApp no abre:** usa el enlace de la pantalla de éxito; el código ya está registrado.
