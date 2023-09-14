#include "../quickjs/quickjs.h"

const char* DATE_POLYFILL = R"lit((() => {
    const _Date = Date;
    // use _Date avoid recursion in _parse.
    const _parse = (date) => {
        if (date === null) {
            // null is invalid
            return new _Date(NaN);
        }
        if (date === undefined) {
            // today
            return new _Date();
        }
        if (date instanceof Date) {
            return new _Date(date);
        }

        if (typeof date === 'string' && !/Z$/i.test(date)) {
            // YYYY-MM-DD HH:mm:ss.sssZ
            const d = date.match(/^(\d{4})[-/]?(\d{1,2})?[-/]?(\d{0,2})[Tt\s]*(\d{1,2})?:?(\d{1,2})?:?(\d{1,2})?[.:]?(\d+)?$/);
            if (d) {
                let YYYY = d[1];
                let MM = d[2] - 1 || 0;
                let DD = d[3] || 1;

                const HH = d[4] || 0;
                const mm = d[5] || 0;
                const ss = d[6] || 0;
                const sssZ = (d[7] || '0').substring(0, 3);

                // Consider that only date strings (such as "1970-01-01") will be processed as UTC instead of local time.
                let utc = (d[4] === undefined) && (d[5] === undefined) && (d[6] === undefined) && (d[7] === undefined);
                if (utc) {
                    return new Date(Date.UTC(YYYY, MM, DD, HH, mm, ss, sssZ));
                }
                return new Date(YYYY, MM, DD, HH, mm, ss, sssZ);
            }
        }

        // everything else
        return new _Date(date);
    };

    const handler = {
        construct: function (target, args) {
            if (args.length === 1 && typeof args[0] === 'string') {
                return _parse(args[0]);
            }

            return new target(...args);
        },
        get(target, prop) {
            if (typeof target[prop] === 'function' && target[prop].name === 'parse') {
                return new Proxy(target[prop], {
                    apply: (target, thisArg, argumentsList) => {
                        if (argumentsList.length === 1 && typeof argumentsList[0] === 'string') {
                            return _parse(argumentsList[0]).getTime();
                        }

                        return Reflect.apply(target, thisArg, argumentsList);
                    }
                });
            } else {
                return Reflect.get(target, prop);
            }
        }
    };

    Date = new Proxy(Date, handler);
})();)lit";


void loadExtendLibraries(JSContext *ctx) {
    JS_Eval(ctx, DATE_POLYFILL, strlen(DATE_POLYFILL), "date-polyfill.js", JS_EVAL_TYPE_GLOBAL);
}