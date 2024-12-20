function fail(message) {
    if (message == null) {
        throw Error("❌assert error.")
    }

    throw Error(message);
}

export function assertEquals(expected, actual) {
    if (expected !== actual) {
        fail(`❌assert failed, expected:[${expected}] but was:[${actual}]`)
    }
}

export function assertTrue(condition) {
    if (!condition) {
        fail()
    }
}

export function assertFalse(condition) {
    if (condition) {
        fail()
    }
}
