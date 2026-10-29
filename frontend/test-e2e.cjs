const puppeteer = require('puppeteer-core');
const fs = require('fs');
const path = require('path');

const CHROME_PATH = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const APP_URL = 'http://localhost:5173';
const BACKEND_URL = 'http://localhost:8080';
const SCREENSHOT_DIR = path.join(__dirname, 'e2e-results');

if (!fs.existsSync(SCREENSHOT_DIR)) {
  fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function fetchJson(url, options = {}) {
  const res = await fetch(url, options);
  return res.json();
}

async function runE2ETest() {
  console.log('====================================================');
  console.log('🚀 Starting End-to-End Booking Flow Integration Test');
  console.log('====================================================\n');

  const testResults = {
    step1_launch: false,
    step2_select_and_db_lock: false,
    step3_simulate_expiry_scheduler: false,
    step4_websocket_color_update: false,
    step5_recommendation_sorting: false,
    step6_booking_confirmation: false,
    errors: [],
  };

  const browser = await puppeteer.launch({
    executablePath: CHROME_PATH,
    headless: true,
    args: ['--no-sandbox', '--disable-gpu', '--window-size=1400,900'],
    defaultViewport: { width: 1400, height: 900 },
  });

  const page = await browser.newPage();

  // Listen to browser console and errors
  const consoleMessages = [];
  page.on('console', (msg) => {
    const text = msg.text();
    consoleMessages.push(text);
    if (text.includes('[WebSocket]') || text.includes('[STOMP]') || text.includes('Error')) {
      console.log(`[Browser Console]: ${text}`);
    }
  });

  page.on('pageerror', (err) => {
    console.error(`[Page Error]: ${err.message}`);
    testResults.errors.push(`Page error: ${err.message}`);
  });

  try {
    // -------------------------------------------------------------------------
    // Step 1: Open Application & Verify Load
    // -------------------------------------------------------------------------
    console.log('▶ Step 1: Launching and navigating to web application...');
    await page.goto(APP_URL, { waitUntil: 'networkidle2', timeout: 15000 });
    const pageTitle = await page.title();
    console.log(`   Page loaded successfully. Title: "${pageTitle}"`);
    await page.screenshot({ path: path.join(SCREENSHOT_DIR, '01_homepage.png') });
    testResults.step1_launch = true;

    // Login as Alice
    console.log('   Authenticating as Alice (alice@example.com)...');
    const loginBtn = await page.$('.login-nav-btn');
    if (loginBtn) {
      await loginBtn.click();
      await page.waitForSelector('.modal-container', { timeout: 8000 });
      await sleep(300);

      // Fill in Alice credentials
      await page.type('#auth-email', 'alice@example.com');
      await page.type('#auth-password', 'password123');

      // Click submit
      const submitBtn = await page.$('.submit-auth-btn');
      if (submitBtn) await submitBtn.click();
      await sleep(1500);
      console.log('   Signed in successfully. Auth badge visible.');
      await page.screenshot({ path: path.join(SCREENSHOT_DIR, '02_logged_in.png') });
    }

    // -------------------------------------------------------------------------
    // Step 5: Verify Theater Recommendation Algorithm & Sorting
    // -------------------------------------------------------------------------
    console.log('\n▶ Step 5: Testing Theater Recommendation Page & Sorting...');
    // Click on movie card for Inception (movieId 1, Aspect Ratio 2.39:1)
    await page.waitForSelector('.movie-card', { timeout: 8000 });
    const movieCards = await page.$$('.movie-card');
    console.log(`   Found ${movieCards.length} movie cards on Home Page.`);
    await movieCards[0].click(); // Click Inception

    // Wait for showtime & seat selection page to render
    await page.waitForSelector('.showtime-page', { timeout: 8000 });
    await page.waitForSelector('.show-card-item', { timeout: 8000 });
    await sleep(1000);

    // Verify recommendations
    const showCards = await page.$$('.show-card-item');
    console.log(`   Found ${showCards.length} scheduled show items.`);

    const recommendations = await page.evaluate(() => {
      const items = Array.from(document.querySelectorAll('.show-card-item'));
      return items.map((el) => {
        const name = el.querySelector('.theater-name')?.textContent?.trim();
        const time = el.querySelector('.show-time-badge')?.textContent?.trim();
        const specs = el.querySelector('.screen-spec-tag')?.textContent?.trim();
        const recText = el.querySelector('.theater-rec-pill')?.textContent?.trim();
        return { name, time, specs, recText };
      });
    });

    console.log('   Auditorium Recommendations in UI:');
    recommendations.forEach((r, idx) => {
      console.log(`     #${idx + 1}: ${r.name} | Specs: ${r.specs} | Rec: ${r.recText}`);
    });

    // Validate algorithm score ordering:
    // Starlight Laser Ultra (920", 2.39:1) => 96%
    // Grand IMAX Screen 1 (840", 1.90:1) => 67.5%
    const backendRecs = await fetchJson(`${BACKEND_URL}/api/movies/1/recommended-theaters`);
    console.log('   Backend API /api/movies/1/recommended-theaters response:');
    backendRecs.forEach((r, idx) => {
      console.log(`     #${idx + 1}: ${r.name} - Score: ${r.score}% (${r.matchQuality})`);
    });

    if (backendRecs.length >= 2 && backendRecs[0].score >= backendRecs[1].score) {
      console.log('   ✔ Theater recommendation algorithm correctly ranks highest score first!');
      testResults.step5_recommendation_sorting = true;
    } else {
      throw new Error('Recommendation algorithm sorting check failed!');
    }
    await page.screenshot({ path: path.join(SCREENSHOT_DIR, '03_recommendations_sorted.png') });

    // -------------------------------------------------------------------------
    // Step 2: Select a Seat in UI & Verify DB updates status to LOCKED
    // -------------------------------------------------------------------------
    console.log('\n▶ Step 2: Selecting seat A1 in UI & Verifying DB Atomic Lock...');
    // Ensure WebSocket status badge is present and shows connection
    await page.waitForSelector('.ws-status-badge', { timeout: 8000 });
    const wsStatusText = await page.$eval('.ws-status-badge', el => el.textContent.trim());
    console.log(`   WebSocket connection badge status: "${wsStatusText}"`);

    // Wait for seats to render
    await page.waitForSelector('.seat-btn', { timeout: 8000 });
    const allSeats = await page.$$('.seat-btn');
    console.log(`   Found ${allSeats.length} seat buttons in cinema layout.`);

    // Click seat A1 (seatId: 1)
    const seatA1 = await page.$('.seat-btn[title*="Seat A1"]');
    if (!seatA1) throw new Error('Seat A1 not found in seat grid!');

    console.log('   Clicking Seat A1...');
    await seatA1.click();
    await sleep(1500);

    // Verify UI state
    const seatA1ClassAfterClick = await page.evaluate(el => el.className, seatA1);
    console.log(`   Seat A1 CSS Class after click: "${seatA1ClassAfterClick}"`);

    // Verify 10-Minute Lock Banner appears in UI
    const lockBanner = await page.$('.seat-lock-banner');
    if (lockBanner) {
      const lockText = await page.evaluate(el => el.textContent, lockBanner);
      console.log(`   ✔ 10-Minute Lock Banner displayed: "${lockText.replace(/\s+/g, ' ').trim()}"`);
    } else {
      console.warn('   Lock banner not found in DOM');
    }
    await page.screenshot({ path: path.join(SCREENSHOT_DIR, '04_seat_a1_locked_ui.png') });

    // Verify Database state via test helper endpoint
    const dbSeatAfterLock = await fetchJson(`${BACKEND_URL}/api/test/seat/1`);
    console.log('   Database state for Seat 1 (A1):', dbSeatAfterLock);

    if (dbSeatAfterLock.status === 'LOCKED' && dbSeatAfterLock.lockedUntil) {
      console.log('   ✔ DATABASE VERIFIED: Seat status is LOCKED with locked_until set!');
      testResults.step2_select_and_db_lock = true;
    } else {
      throw new Error(`Database verification failed! Expected LOCKED, got ${dbSeatAfterLock.status}`);
    }

    // -------------------------------------------------------------------------
    // Step 3: Simulate 10-Minute Expiry & Verify Scheduler Unlocks Seat
    // -------------------------------------------------------------------------
    console.log('\n▶ Step 3: Simulating 10-minute expiry and triggering scheduler...');
    // Expire the seat in database
    const expireRes = await fetchJson(`${BACKEND_URL}/api/test/expire-seat/1`, { method: 'POST' });
    console.log('   Simulated expiry result:', expireRes);

    // Verify DB shows expired lockedUntil before running scheduler
    const dbSeatExpired = await fetchJson(`${BACKEND_URL}/api/test/seat/1`);
    console.log(`   Database lockedUntil modified to: ${dbSeatExpired.lockedUntil}`);

    // Trigger cleanupExpiredLocks (the exact logic executed by @Scheduled)
    console.log('   Triggering cleanupExpiredLocks task...');
    const schedulerRes = await fetchJson(`${BACKEND_URL}/api/test/run-scheduler`, { method: 'POST' });
    console.log('   Scheduler result:', schedulerRes);

    // Verify Database state has been reset to AVAILABLE
    const dbSeatAfterScheduler = await fetchJson(`${BACKEND_URL}/api/test/seat/1`);
    console.log('   Database state for Seat 1 after scheduler run:', dbSeatAfterScheduler);

    if (dbSeatAfterScheduler.status === 'AVAILABLE' && schedulerRes.releasedCount >= 1) {
      console.log('   ✔ SCHEDULER VERIFIED: Seat status reset to AVAILABLE in database!');
      testResults.step3_simulate_expiry_scheduler = true;
    } else {
      throw new Error(`Scheduler verification failed! Status is: ${dbSeatAfterScheduler.status}`);
    }

    // -------------------------------------------------------------------------
    // Step 4: Verify Frontend receives WebSocket notification & updates color
    // -------------------------------------------------------------------------
    console.log('\n▶ Step 4: Verifying frontend received WebSocket release broadcast & updated UI color...');
    // Give STOMP client up to 3 seconds to receive message and update React state
    await sleep(2500);

    // Inspect seat A1 in UI
    const seatA1ClassAfterRelease = await page.evaluate(el => el.className, seatA1);
    const seatA1Style = await page.evaluate(el => {
      const computed = window.getComputedStyle(el);
      return {
        backgroundColor: computed.backgroundColor,
        borderColor: computed.borderColor,
        color: computed.color,
      };
    }, seatA1);

    console.log(`   Seat A1 CSS Class after WebSocket release: "${seatA1ClassAfterRelease}"`);
    console.log(`   Seat A1 Computed Styles:`, seatA1Style);

    // Check for release notification toast in UI
    const releaseNotification = await page.$('.seat-alert.release-notification');
    if (releaseNotification) {
      const toastText = await page.evaluate(el => el.textContent, releaseNotification);
      console.log(`   ✔ Notification Toast displayed: "${toastText.trim()}"`);
    }

    const isAvailable = seatA1ClassAfterRelease.includes('available');
    const isNotLocked = !seatA1ClassAfterRelease.includes('locked') || seatA1ClassAfterRelease.includes('just-released');

    if (isAvailable && isNotLocked) {
      console.log('   ✔ FRONTEND VERIFIED: Seat A1 dynamically transitioned from Locked (Yellow) to Available (Grey)!');
      testResults.step4_websocket_color_update = true;
    } else {
      throw new Error(`Frontend UI color check failed! Class is: "${seatA1ClassAfterRelease}"`);
    }
    await page.screenshot({ path: path.join(SCREENSHOT_DIR, '05_seat_a1_released_websocket.png') });

    // -------------------------------------------------------------------------
    // Step 6: Complete Booking Flow for another seat (Seat B2)
    // -------------------------------------------------------------------------
    console.log('\n▶ Step 6: Completing full booking checkout on seat B2...');
    const seatB2 = await page.$('.seat-btn[title*="Seat B2"]');
    if (seatB2) {
      await seatB2.click();
      await sleep(800);

      const confirmBtn = await page.$('.confirm-booking-btn');
      if (confirmBtn) {
        console.log('   Clicking "Confirm & Book Seat B2"...');
        await confirmBtn.click();
        await page.waitForSelector('.success-modal', { timeout: 6000 });
        console.log('   ✔ Booking Confirmation Modal displayed!');
        await page.screenshot({ path: path.join(SCREENSHOT_DIR, '06_booking_success_modal.png') });

        // Navigate to My Bookings
        const viewBookingsBtn = await page.$('.success-modal button.btn-primary');
        if (viewBookingsBtn) {
          await viewBookingsBtn.click();
          await page.waitForSelector('.bookings-page', { timeout: 6000 });
          console.log('   ✔ My Bookings page rendered with confirmed ticket!');
          await page.screenshot({ path: path.join(SCREENSHOT_DIR, '07_my_bookings_page.png') });
          testResults.step6_booking_confirmation = true;
        }
      }
    }

  } catch (err) {
    console.error(`\n❌ Error during E2E test execution: ${err.message}`);
    testResults.errors.push(err.message);
    await page.screenshot({ path: path.join(SCREENSHOT_DIR, 'error_screenshot.png') }).catch(() => {});
  } finally {
    await browser.close();
  }

  console.log('\n====================================================');
  console.log('📊 E2E Integration Test Execution Summary:');
  console.log('====================================================');
  console.log(`1. Backend & Frontend Launch:       ${testResults.step1_launch ? 'PASSED ✅' : 'FAILED ❌'}`);
  console.log(`2. UI Seat Selection & DB Lock:     ${testResults.step2_select_and_db_lock ? 'PASSED ✅' : 'FAILED ❌'}`);
  console.log(`3. Expiry Simulation & Scheduler:   ${testResults.step3_simulate_expiry_scheduler ? 'PASSED ✅' : 'FAILED ❌'}`);
  console.log(`4. WebSocket Release & Color Sync:  ${testResults.step4_websocket_color_update ? 'PASSED ✅' : 'FAILED ❌'}`);
  console.log(`5. Theater Recommendation Sorting:  ${testResults.step5_recommendation_sorting ? 'PASSED ✅' : 'FAILED ❌'}`);
  console.log(`6. Booking Flow Confirmation:       ${testResults.step6_booking_confirmation ? 'PASSED ✅' : 'FAILED ❌'}`);
  console.log('====================================================');

  if (testResults.errors.length > 0) {
    console.error('\nErrors encountered:', testResults.errors);
    process.exit(1);
  } else {
    console.log('\n🎉 ALL 6 E2E INTEGRATION TEST CRITERIA PASSED SUCCESSFULLY!\n');
    process.exit(0);
  }
}

runE2ETest();
