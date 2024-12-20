function assertDate(expected, actual) {
    if ((Date.parse(expected) === new Date(expected).getTime()) && (Date.parse(expected) === actual)) {
        console.log('✅assert passed with ' + expected, 'Date.parse = ' + Date.parse(expected), 'Date.construct = ' + new Date(expected).getTime(), 'actual = ' + actual);
    } else {
        console.log('❌assert failed with ' + expected, 'Date.parse = ' + Date.parse(expected), 'Date.construct = ' + new Date(expected).getTime(), 'actual = ' + actual);
        throw Error('parse failed.');
    }
}

assertDate('20130108', 1357603200000);
assertDate('2018-04-24', 1524528000000);
assertDate('2018-04-24 11:12', 1524539520000);
assertDate('2018-05-02 11:12:13', 1525230733000);
assertDate('2018-05-02 11:12:13.998', 1525230733998);
assertDate('2018-4-1', 1522540800000);
assertDate('2018-4-1 11:12', 1522552320000);
assertDate('2018-4-1 1:1:1:223', 1522515661223);
assertDate('2018-01', 1514764800000);
assertDate('2018', 1514764800000);
assertDate('2018-05-02T11:12:13Z', 1525259533000);
