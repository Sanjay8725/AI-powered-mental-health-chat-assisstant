const app = require('./app');
require('dotenv').config();

const PORT = process.env.PORT || 5000;

app.listen(PORT, () => {
  console.log(`===============================================`);
  console.log(` MindCare AI Express Backend running on port ${PORT}`);
  console.log(` Health check: http://localhost:${PORT}/health`);
  console.log(` Auth endpoints: http://localhost:${PORT}/api/auth`);
  console.log(` Dataset upload: http://localhost:${PORT}/api/dataset/upload-csv`);
  console.log(`===============================================`);
});
