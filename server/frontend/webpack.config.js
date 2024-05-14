const path = require('path');

const getWebpackConfig = require('@jetbrains/teamcity-api/getWebpackConfig');

module.exports = getWebpackConfig({
  srcPath: path.join(__dirname, './src'),
  outputPath: path.resolve(__dirname, '../src/main/resources/buildServerResources/react'),
  entry: './src/index.tsx',
  useTypeScript: true,
});
