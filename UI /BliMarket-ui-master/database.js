let db = {};

db['open-days'] = require('./data/open-days');
db['graduate-programs'] = require('./data/graduate-programs');
db['code-and-culture'] = require('./data/code-and-culture.json');

module.exports = db;