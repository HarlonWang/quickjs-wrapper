globalThis.console.stdout = (level, msg) => globalThis.print(msg)

const assert = {
    strictEqual: function (actual, expected) {
        if (arguments.length < 2) {
            throw new Error('error missing args');
        }

        if (!Object.is(actual, expected)) {
            throw new Error(`${actual} is not equal ${expected}`)
        }
    }
}

const assertTrue = (actual) => {
    if (!actual) {
        throw new Error(`${actual} is not true.`)
    }
}

// Simple test
assert.strictEqual(format(1), '1');
assert.strictEqual(format(false), 'false');
assert.strictEqual(format('hello'), "'hello'");

// Function test
assert.strictEqual(format(function abc() {}), '[Function: abc]');
assert.strictEqual(format(async function() {}), '[AsyncFunction (anonymous)]');
assert.strictEqual(format(async () => {}), '[AsyncFunction (anonymous)]');
// Special function inspection.
{
    const fn = (() => function*() {})();
    assert.strictEqual(
        format(fn),
        '[GeneratorFunction (anonymous)]'
    );
    assert.strictEqual(
        format(async function* abc() {}),
        '[AsyncGeneratorFunction: abc]'
    );
    Object.defineProperty(fn, 'name', { value: 5, configurable: true });
    assert.strictEqual(
        format(fn),
        '[GeneratorFunction: 5]'
    );
}

assert.strictEqual(format(undefined), "undefined");
assert.strictEqual(format(null), "null");
assert.strictEqual(format(/foo(bar\n)?/gi), '/foo(bar\\n)?/gi');
assert.strictEqual(format([]), '[]');
assert.strictEqual(format([1, 2]), '[ 1, 2 ]');
assert.strictEqual(format([1, [2, 3]]), '[ 1, [ 2, 3 ] ]');
assert.strictEqual(format({}), '{}');
assert.strictEqual(format({ a: 1 }), '{ a: 1 }');
assert.strictEqual(format({ a: function() {} }), '{ a: [Function: a] }');
assert.strictEqual(format({ a: () => {} }), '{ a: [Function: a] }');
// eslint-disable-next-line func-name-matching
assert.strictEqual(format({ a: async function abc() {} }),
    '{ a: [AsyncFunction: abc] }');
assert.strictEqual(format({ a: async () => {} }),
    '{ a: [AsyncFunction: a] }');
assert.strictEqual(format({ a: function*() {} }),
    '{ a: [GeneratorFunction: a] }');
assert.strictEqual(format({ a: 1, b: 2 }), '{ a: 1, b: 2 }');
assert.strictEqual(format({ 'a': {} }), '{ a: {} }');
assert.strictEqual(format({ 'a': { 'b': 2 } }), '{ a: { b: 2 } }');
assert.strictEqual(format({ 'a': { 'b': { 'c': { 'd': 2 } } } }),
    '{ a: { b: { c: [Object] } } }');
assert.strictEqual(
    format({ 'a': { 'b': { 'c': { 'd': 2 } } } }, {depth: null}),
    '{ a: { b: { c: { d: 2 } } } }');
assert.strictEqual(format([1, 2, 3]), '[ 1, 2, 3 ]');
assert.strictEqual(format({ 'a': { 'b': { 'c': 2 } } }, { depth: 0 }),
    '{ a: [Object] }');
assert.strictEqual(format({ 'a': { 'b': { 'c': 2 } } }, { depth: 1 }),
    '{ a: { b: [Object] } }');
assert.strictEqual(format({ 'a': { 'b': ['c'] } }, { depth: 1 }),
    '{ a: { b: [Array] } }');

// Test Map.
{
    assert.strictEqual(format(new Map()), 'Map(0) {}');
    assert.strictEqual(format(new Map([[1, 'a'], [2, 'b'], [3, 'c']])),
        "Map(3) { 1 => 'a', 2 => 'b', 3 => 'c' }");
    const map = new Map([['foo', null]]);
    assert.strictEqual(format(map),
        "Map(1) { 'foo' => null }");
}

// Test circular Map.
{
    const map = new Map();
    map.set(map, 'map');
    assert.strictEqual(
        format(map),
        "Map(1) { [Circular *1] => 'map' }"
    );
    map.set(map, map);
    assert.strictEqual(
        format(map),
        'Map(1) { [Circular *1] => [Circular *1] }'
    );
    map.delete(map);
    map.set('map', map);
    assert.strictEqual(
        format(map),
        "Map(1) { 'map' => [Circular *1] }"
    );
}

// Test multiple circular references.
{
    const obj = {};
    obj.a = [obj];
    obj.b = {};
    obj.b.inner = obj.b;
    obj.b.obj = obj;

    assert.strictEqual(
        format(obj),
        '{' +
        ' a: [ [Circular *1] ],' +
        ' b: { inner: [Circular *2], obj: [Circular *1] } ' +
        '}'
    );
}

// Test Promise.
{
    // quickjs 环境下通过 native 提供的方式获取 Promise 状态
    if (typeof getPromiseState !== "undefined") {
        const resolved = Promise.resolve(3);
        assert.strictEqual(format(resolved), 'Promise { 3 }');

        const rejected = Promise.reject(3);
        assert.strictEqual(format(rejected), 'Promise { <rejected> 3 }');
        // Squelch UnhandledPromiseRejection.
        rejected.catch(() => {});

        const pending = new Promise(() => {});
        assert.strictEqual(format(pending), 'Promise { <pending> }');

        const promiseWithProperty = Promise.resolve('foo');
        assert.strictEqual(format(promiseWithProperty),
            "Promise { 'foo' }");
    }
}

// Truncate output for Primitives with 1 character left
{
    assert.strictEqual(format('bl', { maxStringLength: 1 }),
        "'b'... 1 more character");
}

{
    const x = 'a'.repeat(1e6);
    assertTrue(format(x).endsWith('... 990000 more characters'));
    assert.strictEqual(
        format(x, { maxStringLength: 4 }),
        "'aaaa'... 999996 more characters"
    );
    assertTrue(format(x, { maxStringLength: null }).endsWith(`a'`));
}

{
    assert.strictEqual(
        // eslint-disable-next-line no-loss-of-precision
        format(1234567891234567891234),
        '1.234567891234568e+21'
    );
    assert.strictEqual(
        format(123456789.12345678),
        '123456789.12345678'
    );

    assert.strictEqual(format(10_000_000), '10000000');
    assert.strictEqual(format(1_000_000), '1000000');
    assert.strictEqual(format(100_000), '100000');
    assert.strictEqual(format(99_999.9), '99999.9');
    assert.strictEqual(format(9_999), '9999');
    assert.strictEqual(format(999), '999');
    assert.strictEqual(format(NaN), 'NaN');
    assert.strictEqual(format(Infinity), 'Infinity');
    assert.strictEqual(format(-Infinity), '-Infinity');

    assert.strictEqual(
        format(new Float64Array([100_000_000])),
        'Float64Array(1) [ 100000000 ]'
    );
    assert.strictEqual(
        format(new BigInt64Array([9_100_000_100n])),
        'BigInt64Array(1) [ 9100000100n ]'
    );

    assert.strictEqual(
        format(123456789),
        '123456789'
    );
    assert.strictEqual(
        format(123456789n),
        '123456789n'
    );

    assert.strictEqual(
        format(123456789.12345678),
        '123456789.12345678'
    );

    assert.strictEqual(
        format(-123456789.12345678),
        '-123456789.12345678'
    );
}

// Test es6 Symbol.
{
    assert.strictEqual(format(Symbol()), 'Symbol()');
    assert.strictEqual(format(Symbol(123)), 'Symbol(123)');
    assert.strictEqual(format(Symbol('hi')), 'Symbol(hi)');
    assert.strictEqual(format([Symbol()]), '[ Symbol() ]');
    assert.strictEqual(format({ foo: Symbol() }), '{ foo: Symbol() }');
}

// Test Error.
{
    assert.strictEqual(format(new Error('123')), 'Error: 123')
}

console.log("✅  测试通过")
