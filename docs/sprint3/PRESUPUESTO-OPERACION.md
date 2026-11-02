# Caso de negocio, sección 4: presupuesto de operación

Preparado por Julián, 1 de noviembre de 2026. **Estimación para revisión del equipo**;
no representa recursos contratados ni una factura. El presupuesto preoperativo,
riesgos y viabilidad completos son secciones del caso de negocio del equipo.

## Supuestos y alcance

Una instalación EC2 Linux x86_64, PostgreSQL en el mismo nodo, Nginx, Caddy y ECR;
730 horas por mes. El tipo/región definitivos aún no están elegidos. Para poder
estimar, se muestra un escenario **ilustrativo** con t3.small en Norte de Virginia;
validar memoria/rendimiento y recalcular al elegir la instancia. No se presupone
gratuidad de AWS, créditos estudiantiles ni reservas de capacidad.

Moneda de infraestructura: USD. Conversión presupuestal supuesta **4.000 COP/USD**,
que no es la TRM vigente. Mano de obra y dominio son supuestos propios, pendientes
de cotización. Los gastos de operar este software son distintos de los movimientos
financieros del hotel registrados dentro de la aplicación.

## Costos fijos mensuales del escenario

| Rubro | Base de cálculo | USD/mes | COP/mes |
|---|---|---:|---:|
| Hosting EC2 t3.small | 730 h × USD 0,0209/h | 15,257 | 61.028 |
| Disco EBS gp3, incluida BD | 30 GB × USD 0,08/GB-mes, tarifa usada como supuesto de región | 2,400 | 9.600 |
| Una IPv4 pública | 730 h × USD 0,005/h | 3,650 | 14.600 |
| Imágenes privadas ECR (capacidad prevista) | 2 GB × USD 0,10/GB-mes | 0,200 | 800 |
| Dominio / DNS | Supuesto USD 18/año ÷ 12; confirmar proveedor y zona DNS | 1,500 | 6.000 |
| Soporte preventivo | Supuesto 4 h/mes × COP 30.000/h | 30,000 | 120.000 |
| **Total fijo mensual estimado** | | **53,007** | **212.028** |

La BD no tiene un cobro de RDS en esta arquitectura: comparte CPU/RAM y los 30 GB
presupuestados de EBS. Crecer disco o migrar a RDS exige una nueva estimación.
PostgreSQL, Caddy y Nginx no añaden licencia comercial en este escenario; esto no
elimina el costo de mantenerlos. ECR y EBS se clasifican aquí como capacidad
mensual prevista; al crecer su consumo, crece la factura.

## Costos variables y reserva operativa

| Rubro | Unidad / supuesto de planeación | Costo por unidad | Ejemplo mensual |
|---|---|---|---|
| Transferencia saliente facturable | GB adicional; provisión ilustrativa, confirmar tarifa/franquicias de la cuenta | USD 0,09/GB | 10 GB: USD 0,90 = COP 3.600 |
| Copia externa cifrada | GB-mes; provisión propia hasta elegir servicio/región/retención | USD 0,03/GB-mes | 10 GB: USD 0,30 = COP 1.200 |
| Atención adicional | Hora adicional solicitada | COP 30.000/h, supuesto | 2 h: COP 60.000 |
| CPU excedente / logs / requests de almacenamiento | Según servicio y consumo | Por cotizar; no incluido en el total | Medir antes de cerrar presupuesto |

No hay un costo AWS fijo por reserva de hotel en el diseño actual: depende de
tráfico, almacenamiento y carga. Fórmula mensual del escenario:

`212.028 + GB_salida_facturable × 360 + GB_backup × 120 + horas_soporte_extra × 30.000` COP.

Con 10 GB de salida, 10 GB de respaldo y 2 h adicionales:
**COP 276.828/mes**. Reserva presupuestal del 15 %: COP 41.524,20.
**Presupuesto con reserva: COP 318.352,20/mes**, o COP 3.820.226,40 en 12 meses
si se mantienen los mismos supuestos. No incluye impuestos, cambios de divisa,
cuotas de GitHub si el uso supera lo disponible, ni servicios adicionales.

## Validaciones antes de aprobar la cifra final

Confirmar instancia y región, cotizar dominio/DNS, medir RAM/CPU y espacio de datos,
definir backup externo y retención, validar almacenamiento de fotos y horas reales
de soporte. Verificar créditos de CPU de T3 y política Unlimited. Definir responsable
de pago, presupuesto/alertas de facturación y revisar consumo después de las sesiones.
El esquema de un nodo concentra el riesgo de indisponibilidad; incluirlo en la sección
de riesgos/viabilidad. Las tarifas son referencias de consulta, no una cotización cerrada.

## Fuentes y trazabilidad de precios

Consultadas el 1 de noviembre de 2026:

- [EC2 T3, tabla oficial](https://aws.amazon.com/ec2/instance-types/t3/): t3.small
  Linux/Unix, Norte de Virginia, USD 0,0209/h en la tabla consultada.
- [EBS](https://aws.amazon.com/ebs/pricing/): ejemplo gp3 a USD 0,08/GB-mes;
  comprobar la tarifa efectiva de la región elegida.
- [VPC e IPv4 pública](https://aws.amazon.com/vpc/pricing/): USD 0,005/h por dirección.
- [ECR](https://aws.amazon.com/ecr/pricing/): almacenamiento privado a USD 0,10/GB-mes.
- [AWS Pricing Calculator](https://calculator.aws/): generar y adjuntar la cotización
  del despliegue definitivo. Transferencia y backup de la tabla variable son
  provisiones explícitas, no tarifas confirmadas de un servicio elegido.

Para integrar al caso de negocio: reemplazar el escenario por la cotización elegida,
identificar responsable y fecha de aprobación, y conservar los supuestos y el enlace
a la estimación. El equipo debe validar la viabilidad con sus ingresos/beneficios;
esta sección por sí sola no demuestra viabilidad económica.
