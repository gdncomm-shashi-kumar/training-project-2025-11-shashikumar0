'use strict';

const http = require('http');
const path = require('path');

const express = require('express');
const hbs = require('hbs');
const logger = require('morgan');
const cookieParser = require('cookie-parser');
const bodyParser = require('body-parser');

const routes = require('./routes.js');

let app = express();
let server = http.createServer(app);
let port = process.env.PORT || '3000';

// Environment variables for API endpoints - FORCE PORT 8089 FOR CART AND MEMBER
process.env.MEMBER_SERVICE_URL = process.env.MEMBER_SERVICE_URL || 'http://localhost:8089';
process.env.PRODUCT_SERVICE_URL = process.env.PRODUCT_SERVICE_URL || 'http://localhost:8083';
process.env.CART_SERVICE_URL = process.env.CART_SERVICE_URL || 'http://localhost:8089';

// Ensure all cart and member APIs use port 8089
if (!process.env.CART_SERVICE_URL.includes(':8089')) {
    process.env.CART_SERVICE_URL = 'http://localhost:8089';
}
if (!process.env.MEMBER_SERVICE_URL.includes(':8089')) {
    process.env.MEMBER_SERVICE_URL = 'http://localhost:8089';
}

app.set('port', port);
app.set('views', path.join(__dirname, 'views'));
app.use('/public', express.static(__dirname + '/public'));

// Middleware
app.use(logger('dev'));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));
app.use(cookieParser());

// hbs
app.set('view engine', 'hbs');
hbs.registerPartials(path.join(__dirname, 'views/partials'));

// Custom Handlebars helpers
hbs.registerHelper('json', function(context) {
    return JSON.stringify(context);
});

hbs.registerHelper('eq', function(a, b) {
    return a === b;
});

hbs.registerHelper('currency', function(value) {
    return '$' + parseFloat(value).toFixed(2);
});

hbs.registerHelper('multiply', function(a, b) {
    return a * b;
});

hbs.registerHelper('math', function(a, operator, b) {
    a = parseFloat(a);
    b = parseFloat(b);
    switch(operator) {
        case '+': return a + b;
        case '-': return a - b;
        case '*': return a * b;
        case '/': return a / b;
        case '%': return a % b;
        default: return 0;
    }
});

hbs.registerHelper('gt', function(a, b) {
    return a > b;
});

hbs.registerHelper('and', function(a, b) {
    return a && b;
});

app.use('/', routes);

// Error handling middleware
app.use((err, req, res, next) => {
    console.error('Error:', err);
    res.status(err.status || 500);
    res.render('pages/error', {
        title: 'Error',
        error: {
            status: err.status || 500,
            message: err.message || 'Internal Server Error'
        }
    });
});

// 404 handler
app.use((req, res) => {
    res.status(404).render('pages/error', {
        title: 'Page Not Found',
        error: {
            status: 404,
            message: 'The page you are looking for does not exist.'
        }
    });
});

server.listen(app.get('port'), () => {
    console.log(`🚀 BliMarket UI is now running on port ${app.get('port')}`);
    console.log(`📦 Product Service: ${process.env.PRODUCT_SERVICE_URL}`);
    console.log(`👤 Member Service: ${process.env.MEMBER_SERVICE_URL}`);
    console.log(`🛒 Cart Service: ${process.env.CART_SERVICE_URL}`);
});