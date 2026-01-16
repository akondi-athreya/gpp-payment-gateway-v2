const path = require('path');

module.exports = [
  // SDK Bundle (embed in merchant site)
  {
    name: 'sdk',
    mode: 'production',
    entry: './src/sdk/index.js',
    output: {
      path: path.resolve(__dirname, 'dist'),
      filename: 'checkout.js',
      library: 'PaymentGateway',
      libraryTarget: 'umd',
      globalObject: 'this'
    },
    module: {
      rules: [
        {
          test: /\.js$/,
          exclude: /node_modules/,
          use: {
            loader: 'babel-loader',
            options: {
              presets: ['@babel/preset-env']
            }
          }
        },
        {
          test: /\.css$/,
          use: ['style-loader', 'css-loader']
        }
      ]
    }
  },
  // Iframe Bundle (served inside iframe)
  {
    name: 'iframe',
    mode: 'production',
    entry: './src/iframe-content/index.jsx',
    output: {
      path: path.resolve(__dirname, 'dist'),
      filename: 'checkout-iframe.js'
    },
    module: {
      rules: [
        {
          test: /\.(js|jsx)$/,
          exclude: /node_modules/,
          use: {
            loader: 'babel-loader',
            options: {
              presets: ['@babel/preset-env', '@babel/preset-react']
            }
          }
        },
        {
          test: /\.css$/,
          use: ['style-loader', 'css-loader']
        }
      ]
    }
  }
];
