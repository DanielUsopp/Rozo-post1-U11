# Refactoring U11 — Refactorización Guiada por Pruebas

## Descripción
Repositorio del post-contenido de la Unidad 11 de Patrones de Diseño de Software. Un único proyecto Spring Boot (`refactoring-u11`, solo `spring-boot-starter`) con un servicio de inventario que parte con dos code smells reales. La Parte 1 blinda el comportamiento con pruebas de caracterización y después elimina los dos smells, verificando con esas mismas pruebas que el comportamiento observable no cambió. La Parte 2 agrega un módulo de reportes cuya integración crea un ciclo entre paquetes, lo detecta con ArchUnit, lo resuelve con inversión de dependencias y vuelve a correr las pruebas de la Parte 1 sin modificarlas.

## Parte 1 — Red de seguridad y refactorización guiada por evidencia

### Orden de trabajo
| Commit | Contenido | Resultado de `mvn test` |
|--------|-----------|--------------------------|
| `feat: crear inventario con Long Method...` | Código original con los dos smells | — (sin pruebas) |
| `test: agregar pruebas de caracterizacion...` | 22 pruebas de caracterización sobre el código original | 23 pruebas, 0 fallos |
| `refactor: eliminar Long Method...` | Descomposición de `actualizarInventario()` y `CalculadoraReorden` | 25 pruebas, 0 fallos |
| `refactor: eliminar switch duplicado...` | Polimorfismo en `TipoProducto` | 25 pruebas, 0 fallos |

Las 23 pruebas iniciales son las 22 de caracterización más `contextLoads`, la prueba que genera Spring Initializr. Las dos que se suman en el primer refactor son de `CalculadoraReordenTest`, la clase nueva. `InventarioServiceTest`, `ClasificadorProductoTest` y `EtiquetadoProductoTest` no se modificaron después del commit de caracterización (`git diff 6057c48 -- src/test` sobre esos tres archivos está vacío).

### Pruebas de caracterización
Además de las pruebas del enunciado, se fijaron los casos límite que un refactor podía romper sin que nadie lo notara:

- `salida_dejaStockExactamenteEnElMinimo_noActivaReorden`: la comparación es "menor que", no "menor o igual".
- `salida_dejaStockEnCero_reordenaElTripleDelMinimo` y `entrada_queNoAlcanzaElMinimo_tambienActivaReorden`: el reorden también aplica en entradas.
- `cantidadCero_...` y `productoIdNulo_...`: las otras dos ramas de la validación.
- `tipoMovimientoNulo_lanzaNullPointerException`: el código original no valida `null` y falla con una excepción.
- `entrada_conCantidadNegativa_noSeValidaYPuedeDejarStockNegativo`: solo `SALIDA` revisa que el stock no quede negativo; una `ENTRADA` de −30 deja el stock en −10.

Los dos últimos casos documentan comportamientos que no son deseables, pero son los reales. Una prueba de caracterización registra lo que el código hace hoy, no lo que debería hacer: corregirlos sería un cambio de comportamiento y no un refactor, y queda para otra tarea. Gracias a esas dos pruebas, la versión refactorizada sigue usando `tipoMovimiento.equals(...)` y solo revisa el stock negativo en salidas; una versión "más limpia" que validara todo de una vez habría roto ambas.

En `ClasificadorProductoTest` se agregó `alimento_recargoNoDependeDelPrecio` (un alimento de 300 paga el mismo 2,50) y en `EtiquetadoProductoTest` los casos de `ROPA` y `ALIMENTO`, para cubrir las cuatro ramas del `switch` en las dos clases.

### Smell 1 — Long Method en `actualizarInventario()`
**Evidencia antes.** El método tenía 44 líneas y complejidad ciclomática 13 (medida con JaCoCo), y mezclaba cinco responsabilidades marcadas con comentarios: validar la entrada, buscar el producto, actualizar el stock, calcular el reorden y registrar el movimiento o emitir la alerta.

**Técnica: Extract Method + Extract Class.** Cada comentario del método original marcaba un bloque con una responsabilidad propia, que es la señal para Extract Method (Guía, sección 2). El cálculo de reorden se extrajo además a su propia clase (ver Decisión de diseño 1).

**Evidencia después.**
```java
public String actualizarInventario(Long productoId, int cantidad, String tipoMovimiento) {
    if (datosDeEntradaInvalidos(productoId, cantidad, tipoMovimiento)) {
        return "ERROR_DATOS_INVALIDOS";
    }
    Optional<Producto> encontrado = buscarPorId(productoId);
    if (encontrado.isEmpty()) {
        return "ERROR_PRODUCTO_NO_EXISTE";
    }
    return moverStock(encontrado.get(), cantidad, tipoMovimiento);
}
```

| Método | Responsabilidad | Complejidad ciclomática |
|--------|-----------------|-------------------------|
| `actualizarInventario` | Coordinar (10 líneas) | 3 |
| `datosDeEntradaInvalidos` | Validar la entrada | 5 |
| `buscarPorId` | Buscar el producto (devuelve `Optional`, nunca `null`) | 1 |
| `moverStock` | Aplicar la entrada o la salida | 5 |
| `registrarMovimiento` | Auditoría | 1 |
| `alertarBajoStock` | Alerta y código de retorno | 1 |
| `CalculadoraReorden.necesitaReorden` / `unidadesAReordenar` | Política de reabastecimiento | 2 / 2 |

El método principal pasó de 44 a 10 líneas y de CC 13 a 3. Usa guard clauses (retornos tempranos) en lugar de condicionales anidados: se lee de arriba abajo, cada `if` corta un caso de error y el camino normal queda al final.

### Smell 2 — Switch Statements duplicados sobre `TipoProducto`
**Evidencia antes.** `ClasificadorProducto.calcularRecargoEnvio()` y `EtiquetadoProducto.generarEtiqueta()` tenían un `switch` de 8 líneas cada uno, con complejidad ciclomática 5 en ambos. Los dos bloques tenían las mismas cuatro etiquetas `case` en el mismo orden y el mismo `default`. Además, 4 de las 8 líneas eran idénticas carácter por carácter: el `switch`, las dos líneas del `default` y la llave de cierre. Agregar un tipo nuevo obligaba a acordarse de editar las dos clases; si se olvidaba una, el error solo aparecía en ejecución (`IllegalArgumentException` del `default`).

**Técnica: Replace Conditional with Polymorphism**, con el comportamiento dentro del propio enum.

**Evidencia después.**
```java
// ClasificadorProducto
public double calcularRecargoEnvio(Producto producto) {
    return producto.getTipo().recargoEnvio(producto.getPrecio());
}
// EtiquetadoProducto
public String generarEtiqueta(Producto producto) {
    return producto.getTipo().etiquetaPara(producto.getNombre());
}
```
No queda ningún `switch` ni `if` sobre el tipo de producto en el proyecto (`grep -rn "switch\|case " src/main/java` no devuelve resultados). Las dos firmas originales se conservaron. Agregar un tipo nuevo significa agregar una constante con su prefijo, su leyenda y su recargo; el compilador obliga a implementar `recargoEnvio` y ninguna de las dos clases cambia.

## Decisiones de diseño

### Decisión de diseño 1 — El cálculo de reorden como clase colaboradora (`CalculadoraReorden`)
Se eligió una clase propia y no un método privado. La pregunta del enunciado es si esa lógica tiene una razón de cambio distinta de la del resto del servicio, y la tiene: cuánto y cuándo reabastecer es una política de compras (hoy "hasta el triple del mínimo"), que puede cambiar por razones que no tienen nada que ver con registrar entradas y salidas, como el tiempo de entrega del proveedor o la demanda de la temporada. Como clase, esa política tiene nombre propio, se prueba sola (`CalculadoraReordenTest`, sin crear un inventario) y el día que cambie se toca un solo archivo.

Se consideró también moverla a `Producto` (el cálculo solo usa datos del producto, una señal de Feature Envy). Se descartó porque ataría una política de compras a la entidad, y la entidad cambiaría cada vez que compras cambie de criterio.

`InventarioService` crea su `CalculadoraReorden` en la declaración del campo, en lugar de recibirla por constructor. Es deliberado: las pruebas de caracterización usan `new InventarioService()` y no se podían modificar. Es un `new` aceptable porque la calculadora no tiene estado ni dependencias externas, así que no impide probar el servicio. Si en el futuro la política dependiera de algo externo, como un servicio de proveedores, se pasaría a inyección por constructor.

### Decisión de diseño 2 — Enum con comportamiento por constante, no una interfaz de estrategia con una clase por tipo
Se eligió el enum porque el conjunto de tipos es cerrado y estable en este dominio, y el enum hace que el compilador obligue a cada tipo nuevo a declarar su comportamiento. Una interfaz con cuatro clases (una por tipo) agregaría cuatro archivos y una forma de mapear cada `TipoProducto` a su clase, y solo se justificaría si cada tipo necesitara colaboradores inyectados por Spring, por ejemplo una tabla de tarifas en base de datos. Hoy ninguno los necesita.

Dentro del enum se distinguió entre lo que es **dato** y lo que es **comportamiento**:
- **La etiqueta es dato.** Las cuatro tienen el mismo formato `[PREFIJO] nombre - leyenda`, así que cada constante guarda su prefijo y su leyenda y el formato vive en un solo método, `etiquetaPara`. Escribir cuatro métodos que solo cambian un texto habría repetido el formato cuatro veces.
- **El recargo es comportamiento.** Tres tipos lo calculan como porcentaje del precio y `ALIMENTO` cobra un monto fijo que no depende del precio. Por eso es un método abstracto que cada constante implementa.

## Parte 2 — Diagnóstico del acoplamiento entre paquetes

### El ciclo
El módulo `reportes` se integró como pide el enunciado: `InventarioService` recibe por setter un `ReporteBajoStockService` para avisarle cuando un producto queda bajo el mínimo, y `ReporteBajoStockService` recibe por constructor el `InventarioService` para consultar esos productos. El resultado son dos flechas en sentidos opuestos:

```
   ┌──────────────────────┐   importa ReporteBajoStockService   ┌──────────────────────┐
   │      inventario      │ ──────────────────────────────────▶ │       reportes       │
   │  InventarioService   │                                      │ ReporteBajoStock-    │
   │                      │ ◀────────────────────────────────── │ Service              │
   └──────────────────────┘     importa InventarioService        └──────────────────────┘
```

### Evidencia
Salida de `mvn test -Dtest=ArquitecturaPaquetesTest` en este punto:

```
Architecture Violation [Priority: MEDIUM] - Rule 'slices matching 'com.universidad.refactoringu11.(*)..' should be free of cycles' was violated (1 times):
Cycle detected: Slice inventario ->
                Slice reportes ->
  1. Dependencies of Slice inventario
    - Field <...inventario.InventarioService.reporteBajoStockService> has type <...reportes.ReporteBajoStockService>
    - Method <...inventario.InventarioService.alertarBajoStock(...)> calls method <...reportes.ReporteBajoStockService.generarReporte()>
  2. Dependencies of Slice reportes
    - Constructor <...reportes.ReporteBajoStockService.<init>(...inventario.InventarioService)> has parameter of type <...inventario.InventarioService>
    - Method <...reportes.ReporteBajoStockService.generarReporte()> calls method <...inventario.InventarioService.productosBajoStockMinimo()>
```

Las pruebas de negocio de la Parte 1 (`InventarioServiceTest`, `ClasificadorProductoTest`, `EtiquetadoProductoTest`) siguen en verde con el ciclo presente: instancian `InventarioService` con `new` y nunca le asignan el colaborador, así que el `if (reporteBajoStockService != null)` evita que se note. El problema no es de comportamiento, es de estructura.

Hubo una segunda evidencia que el enunciado no menciona: `contextLoads`, la prueba que genera Spring Initializr, empezó a fallar desde el commit del módulo de reportes. Spring no puede arrancar la aplicación porque los dos beans se necesitan mutuamente:

```
The dependencies of some of the beans in the application context form a cycle:
   inventarioService
      ↓
   reporteBajoStockService
Relying upon circular references is discouraged and they are prohibited by default.
```

ArchUnit detecta el ciclo entre paquetes leyendo el bytecode; Spring detecta el ciclo entre objetos al intentar construirlos. Son dos problemas relacionados pero distintos, y la Parte 2 tiene que resolver los dos.

## Parte 2 — Resolución

### Paso 1: invertir la dependencia entre paquetes
Se extrajo la interfaz `NotificadorBajoStock` en el paquete `inventario`. `InventarioService` dejó de importar cualquier clase de `reportes` y ahora depende solo de la interfaz (`setNotificador`, con `@Autowired(required = false)` para no tocar el constructor que usan las pruebas). `ReporteBajoStockService` implementa la interfaz, de modo que la única flecha que queda va de `reportes` a `inventario`:

```
   ┌────────────────────────────────┐                     ┌──────────────────────────┐
   │           inventario           │                     │         reportes         │
   │  InventarioService             │                     │                          │
   │      │ usa                     │   implementa        │  ReporteBajoStockService │
   │      ▼                         │ ◀────────────────── │                          │
   │  «interface»                   │                     │  AlertaReorden           │
   │  NotificadorBajoStock          │                     │                          │
   └────────────────────────────────┘                     └──────────────────────────┘
```

Con este commit `ArquitecturaPaquetesTest` pasó a verde:

```
$ mvn test -Dtest=ArquitecturaPaquetesTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Paso 2: el ciclo que la prueba de arquitectura no ve
Con los paquetes ya sin ciclo, `contextLoads` seguía fallando. En el código del enunciado, `ReporteBajoStockService` conserva su constructor con `InventarioService` para consultar los productos bajo el mínimo, así que en ejecución el ciclo seguía ahí: `InventarioService → NotificadorBajoStock (que es ReporteBajoStockService) → InventarioService`. ArchUnit no lo marca porque la regla es sobre paquetes y la flecha `reportes → inventario` está permitida; Spring sí, porque construye objetos.

Se resolvió quitando la causa en vez de esconderla. El reporte ya no le pregunta nada al inventario: cada notificación trae el producto y la cantidad a reordenar, y `ReporteBajoStockService` guarda la última alerta de cada producto (`AlertaReorden`) para armar el reporte. Con eso desaparecieron el constructor con `InventarioService` y el método `productosBajoStockMinimo()`, que solo existía para esa consulta. Se descartaron dos alternativas que hacían arrancar la aplicación sin cambiar el diseño: `spring.main.allow-circular-references=true`, que el propio mensaje de Spring califica como último recurso, y `@Lazy` sobre la dependencia, que pospone la creación del bean pero deja el ciclo intacto.

`NotificacionBajoStockContextoTest` (`@SpringBootTest`) confirma el flujo completo con el contexto real: una salida que deja un producto bajo el mínimo devuelve `OK_BAJO_STOCK_REORDEN_9` y el reporte contiene la línea de ese producto.

### Paso 3: las pruebas de la Parte 1 sin modificar
```
$ mvn test
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in ...ArquitecturaPaquetesTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in ...reportes.ReporteBajoStockServiceTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in ...NotificacionBajoStockContextoTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in ...RefactoringU11ApplicationTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in ...inventario.CalculadoraReordenTest
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0 -- in ...inventario.InventarioServiceTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in ...inventario.ClasificadorProductoTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in ...inventario.EtiquetadoProductoTest
[INFO] Tests run: 29, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
Las 22 pruebas de caracterización pasan sin haberse tocado desde el commit `6057c48`. La reestructuración de paquetes no cambió el comportamiento observable del inventario.

### Historial de `mvn test` en la Parte 2
| Commit | ArquitecturaPaquetesTest | contextLoads | Pruebas de la Parte 1 |
|--------|--------------------------|--------------|------------------------|
| `feat: agregar modulo de reportes...` | (aún no existe) | Falla: ciclo de beans | Verde |
| `test: agregar prueba de arquitectura... (en rojo)` | Falla: ciclo inventario ↔ reportes | Falla | Verde |
| `refactor: invertir la dependencia...` | Verde | Falla: el ciclo sigue entre objetos | Verde |
| `fix: romper el ciclo entre beans...` | Verde | Verde | Verde |

### Decisión de diseño 3 — `NotificadorBajoStock` vive en `inventario`
La regla de la guía (sección 4.3) es que los paquetes de infraestructura dependen del dominio y el dominio nunca depende de infraestructura. En este proyecto `inventario` es el dominio y el paquete de mayor nivel: tiene las reglas de negocio (stock, reorden, tipos de producto) y no le debería importar quién se entera de un bajo stock ni qué hace con eso. `reportes` es un consumidor de esos eventos, de menor nivel: mañana podría haber otro, como un correo a compras, sin que `inventario` cambie. La interfaz expresa lo que el dominio necesita ("avisar que un producto está bajo el mínimo y cuánto reordenar"), así que pertenece a quien la usa y no a quien la implementa. Si se hubiera puesto en `reportes`, `InventarioService` seguiría importando `reportes` y el ciclo no se habría roto, solo se habría movido de clase.

### Decisión de diseño 4 — Notificación por evento individual
Se mantuvo `notificar(Producto, int cantidadReorden)`, un aviso por producto en el momento en que cruza el mínimo, y no `notificar(List<Producto>)`.

- **Ventajas en este diseño.** El aviso sale exactamente donde ocurre el hecho, dentro de `alertarBajoStock`, sin que el inventario tenga que acumular nada ni decidir cuándo "cerrar" un lote. Además, cada aviso trae los datos completos (producto y cantidad), y eso fue lo que permitió quitar la consulta de `reportes` hacia `inventario` y romper el ciclo de beans. Con notificaciones por lote, el inventario necesitaría un búfer y un disparador (un temporizador o un fin de proceso), es decir, más estado y más código en el dominio.
- **Cuándo convendría por lote.** Cuando cada aviso tiene un costo alto o el destinatario prefiere un resumen: por ejemplo, si el notificador enviara un correo a compras o llamara a la API de un proveedor, cien salidas seguidas generarían cien mensajes; un resumen diario o un cierre de inventario nocturno con `notificar(List<Producto>)` sería más razonable. También cuando el consumidor tiene que ver todos los productos juntos para decidir, como una orden de compra consolidada por proveedor.

## Ejecutar las pruebas
```bash
mvn test
```

## Herramientas utilizadas
- Java 17, Spring Boot 3.3, JUnit 5, ArchUnit 1.3.0
- JaCoCo (medición de complejidad por método), Apache Maven, Git, GitHub

## Conclusiones
Escribir primero las pruebas de caracterización cambió la forma de refactorizar: no se trataba de dejar el código "como debería ser", sino de cambiar su estructura sin alterar lo que hace, incluidos comportamientos poco deseables como aceptar una entrada negativa. Esas pruebas fueron las que obligaron a conservar detalles que un refactor descuidado habría cambiado. En el switch duplicado, la decisión más útil fue separar lo que es dato (los textos de la etiqueta) de lo que es comportamiento (la fórmula del recargo), en lugar de convertir todo en métodos. La Parte 2 mostró que un ciclo puede existir en dos niveles: ArchUnit lo encontró entre paquetes y la inversión de dependencias lo resolvió, pero el ciclo entre objetos solo desapareció cuando el reporte dejó de consultar al inventario y pasó a usar los datos que ya traía cada notificación.
