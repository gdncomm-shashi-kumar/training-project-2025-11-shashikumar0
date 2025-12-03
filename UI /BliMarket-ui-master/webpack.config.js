const path = require('path');

const ExtractTextPlugin = require('extract-text-webpack-plugin');
const NodemonPlugin = require('nodemon-webpack-plugin');

let extractSCSS = new ExtractTextPlugin({
    filename: 'styles.min.css'
});

module.exports = {
    entry: './public/js/main.js',
    output: {
        path: path.resolve(__dirname, 'public/dist'),
        filename: 'scripts.min.js',
        publicPath: '/public/dist'
    },
    module: {
        rules: [
            {
                test: /\.js$/,
                use: [
                    {
                        loader: 'babel-loader',
                        options: {
                            presets: ['env']
                        }
                    }
                ]
            },
            {
                test: /\.scss$/,
                use: extractSCSS.extract({
                    use: ['css-loader', 'sass-loader']
                })
            }
        ]
    },
    plugins: [
        new NodemonPlugin({
            // What to watch (everything)
            watch: path.resolve('./'),
            // Detailed log
            verbose: true,
            // Nodemon arguments
            nodeArgs: ['server.js']
        }),
        extractSCSS
    ]
};