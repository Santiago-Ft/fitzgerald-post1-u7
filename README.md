# Post-contenido — Unidad 7: Patrones Arquitectónicos I

## Descripción
Repositorio del post-contenido de la Unidad 7 de Patrones de Diseño
de Software. Un unico proyecto Spring Boot (multas-biblioteca-api)
para la gestion de multas de biblioteca, con dos partes: una API REST
en capas (Model, Repository, Service, Controller) sobre H2, y pago en
linea de multas con dos pasarelas intercambiables.

## Parte 1 — Arquitectura en Capas
MultaRepository extiende JpaRepository y agrega una consulta
agregada (countByEstudianteIdAndEstado). MultaService concentra las
reglas de negocio (limite de multas pendientes, generacion); el
calculo del monto vive en la propia entidad Multa
(Multa.calcularMonto). MultaController expone /api/multas. Ver
paquetes model/, repository/, service/ y controller/.

## Parte 2 — Pago en Linea con Dos Pasarelas
[Documentar aqui la opcion elegida entre A (rama condicional), B
(Strategy en service/) o C (puerto de dominio con adaptadores) y por
que. Si se eligio C: domain/port/PasarelaPagoPort y domain/ResultadoPago
son Java puro; infrastructure/pago/PagosUdesAdapter e
infrastructure/pago/WompiAdapter traducen cada formato HTTP externo
al mismo ResultadoPago; se seleccionan por app.pagos.proveedor sin
tocar MultaController ni el resto de MultaService.]

## Cómo ejecutar
```
$ cd multas-biblioteca-api && mvn spring-boot:run
```

## Herramientas utilizadas
- Java 17, Spring Boot 3.x, Spring Data JPA, H2, RestTemplate
- Apache Maven, Postman/curl, Git, GitHub

## Decisiones de diseño

### Punto de decisión 1 — Cálculo del monto: ¿entidad o Service?
La definición de "Multa.calcularMonto(int diasAtraso)" directamente dentro de la entidad "Multa" es una decisión arquitectónica correcta, ya que responde a una regla de negocio pura que no requiere colaboradores externos como repositorios o beans de Spring. Las fuentes establecen que las reglas cuyo cálculo depende únicamente de los parámetros propios del objeto deben vivir en el propio dominio para otorgarle comportamiento activo. Ubicar esta fórmula en "MultaService" habría dejado a la entidad como un simple contenedor pasivo de datos con getters y setters, incurriendo en el antipatrón de modelo de dominio anémico. Mantener la regla en la entidad asegura alta cohesión, evita la duplicación del cálculo y permite reutilizar la lógica de negocio sin forzar dependencias innecesarias hacia la capa de servicio.

### Punto de decisión 2 — Conteo de multas pendientes: ¿consulta o filtrado en memoria?
La verificación de multas pendientes mediante la consulta derivada "countByEstudianteIdAndEstado" en "MultaRepository" es la estrategia idónea para equilibrar rendimiento y separación de responsabilidades. A diferencia del cálculo del monto, determinar si un estudiante acumula multas requiere consultar información del estado persistido que solo la base de datos conoce. En este esquema, MultaService mantiene el control de la decisión de negocio (evaluar si el número de multas alcanza el límite permitido para rechazar o autorizar la transacción), pero delega la agregación al motor de base de datos para ejecutar un "COUNT" nativo en SQL. Cargar colecciones completas en memoria mediante "findByEstudianteId" para filtrarlas con *ava Streams degradaría el tiempo de respuesta progresivamente a medida que el historial del estudiante crezca.

### Punto de decisión 3 — Selección del adaptador activo
Uso de la anotación @ConditionalOnProperty en cada adaptador ("PagosUdesAdapter" y "WompiAdapter"). Resuelve la selección de la pasarela en tiempo de arranque. Solo un bean que implementa PasarelaPagoPort existe en el contexto de Spring a la vez. MultaService inyecta directamente la interfaz por constructor sin requerir "@Qualifier" ni estructuras condicionales en runtime. Inyectar un "Map<String, PasarelaPagoPort>" para elegir dinámicamente en tiempo de ejecución daría flexibilidad, pero obligaría a MultaService a conocer claves y lógica de configuración del entorno de infraestructura.

### Punto de decisión 4 — Diseño del puerto y el tipo de resultado
Creación del DTO neutro de dominio ResultadoPago con campos estandarizados (proveedor, exitoso, referenciaExterna, mensaje). Aísla el núcleo de la aplicación de las particularidades de API externas (como "idTransaccion" de "PagosUDES" o el monto en centavos y reference de Wompi). Si "ResultadoPago" tuviera un campo acoplado a un proveedor específico (como "idTransaccion"), el segundo adaptador tendría que acomodar o falsear sus respuestas. Cualquier pasarela adicional obligaría a modificar los DTOs de dominio y la lógica interna de MultaService.

### Trade-off considerado — Parte 2
Para solucionar el pago en línea se compararon la extensión directa mediante un bloque condicional en el servicio, el patrón Strategy en la capa Service y la introducción de un Puerto de Dominio con Adaptadores en infraestructura. Se eligió la Opción C debido a que cada pasarela maneja contratos REST y formatos de datos sustancialmente diferentes.

Se genero un desacoplamiento total, MultaService desconoce los detalles HTTP, JSON o conversiones de moneda. Es posible cambiar, agregar o remover pasarelas creando adaptadores sin modificar una sola línea de código en las capas de aplicación o presentación.

Aumento en la complejidad estructural del proyecto (creación de paquetes adicionales domain/ e infrastructure/, interfaces extra y clases DTO de mapeo). Si el piloto terminara y la universidad estableciera una única pasarela permanente, la indirección de un puerto resultaría excesiva; sin embargo, para el escenario actual de múltiples proveedores en evaluación, el beneficio supera ampliamente el costo.

## Conclusiones
La arquitectura de capas es perfecta para desarrollo de operaciones CRUDs y reglas de negocio centradas en bases de datos. Ademas de añadir adaptadores y puertos externos aumentan la proteccion del dominio sin añadir complejidad.
