// Test Realty In US API Connection
// Run with: node test-realty-api.js

const https = require('https');
const path = require('path');

const data = JSON.stringify({
  limit: 10,
  offset: 0,
  postal_code: '02215', // Boston area
  status: ['for_sale'],
  sort: {
    direction: 'desc',
    field: 'list_date'
  }
});

const RAPIDAPI_KEY = process.env.RAPIDAPI_KEY || process.env.REALTY_API_KEY || 'YOUR_RAPIDAPI_KEY';

const options = {
  hostname: 'realty-in-us.p.rapidapi.com',
  path: '/properties/v3/list',
  method: 'POST',
  headers: {
    'x-rapidapi-key': RAPIDAPI_KEY,
    'x-rapidapi-host': 'realty-in-us.p.rapidapi.com',
    'Content-Type': 'application/json',
    'Content-Length': data.length
  }
};

console.log('===================================================');
console.log('Testing Realty In US API Connection');
console.log('===================================================\n');
console.log('Searching for properties in Boston (02215)...\n');

if (RAPIDAPI_KEY === 'YOUR_RAPIDAPI_KEY') {
  console.warn('Warning: RAPIDAPI_KEY environment variable is not set. Set RAPIDAPI_KEY before running this script.');
}

const req = https.request(options, (res) => {
  let responseData = '';

  res.on('data', (chunk) => {
    responseData += chunk;
  });

  res.on('end', () => {
    console.log(`Status Code: ${res.statusCode}\n`);
    
    if (res.statusCode === 200) {
      try {
        const parsed = JSON.parse(responseData);
        console.log('✅ API Connection Successful!\n');
        console.log(`Total Properties Found: ${parsed.data?.home_search?.count || 0}\n`);
        
        if (parsed.data?.home_search?.results?.length > 0) {
          console.log('First 3 Properties:\n');
          parsed.data.home_search.results.slice(0, 3).forEach((prop, idx) => {
            console.log(`${idx + 1}. ${prop.location?.address?.line || 'N/A'}`);
            console.log(`   Price: $${prop.list_price?.toLocaleString() || 'N/A'}`);
            console.log(`   Beds: ${prop.description?.beds || 'N/A'}, Baths: ${prop.description?.baths || 'N/A'}`);
            console.log(`   Sqft: ${prop.description?.sqft || 'N/A'}`);
            console.log(`   Property ID: ${prop.property_id || 'N/A'}\n`);
          });
          
          console.log('Available Financial Data Fields:');
          const firstProp = parsed.data.home_search.results[0];
          console.log('- list_price:', firstProp.list_price ? '✓' : '✗');
          console.log('- price_per_sqft:', firstProp.price_per_sqft ? '✓' : '✗');
          console.log('- tax_history:', firstProp.tax_history ? '✓' : '✗');
          console.log('- hoa:', firstProp.hoa ? '✓' : '✗');
          console.log('- rent_estimate:', firstProp.rent_estimate ? '✓' : '✗');
        }
        
        // Save sample response
  const fs = require('fs');
  const outputPath = path.join(__dirname, '..', 'docs', 'realty-api-sample-response.json');
  fs.writeFileSync(outputPath, JSON.stringify(parsed, null, 2));
  console.log(`\n📄 Full response saved to: ${outputPath}`);
        
      } catch (err) {
        console.error('❌ Error parsing response:', err.message);
        console.log('Raw response:', responseData.substring(0, 500));
      }
    } else {
      console.error('❌ API Request Failed');
      console.log('Response:', responseData);
    }
    
    console.log('\n===================================================');
  });
});

req.on('error', (err) => {
  console.error('❌ Request Error:', err.message);
});

req.write(data);
req.end();
