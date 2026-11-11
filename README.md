# Refactoring U11 — Refactorización Guiada por Pruebas

## Descripción
Repositorio del post-contenido de la Unidad 11 de Patrones de Diseño de Software. Un único proyecto Spring Boot (`refactoring-u11`, solo `spring-boot-starter`) con un servicio de inventario que parte con dos code smells reales. La Parte 1 blinda el comportamiento con pruebas de caracterización y después elimina los dos smells, verificando con esas mismas pruebas que el comportamiento observable no cambió.

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

## Ejecutar las pruebas
```bash
mvn test
```

## Herramientas utilizadas
- Java 17, Spring Boot 3.3, JUnit 5
- JaCoCo (medición de complejidad por método), Apache Maven, Git, GitHub
