package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.SimpleBuyAnalytics
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.swap.nabu.NabuToken
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_simple_buy_intro.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.ErrorDialogData
import piuk.blockchain.android.ui.base.ErrorSlidingBottomDialog
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible

class SimpleBuyIntroFragment : Fragment(), SimpleBuyScreen {

    private val nabuToken: NabuToken by scopedInject()
    private val simpleBuyPrefs: SimpleBuyPrefs by inject()
    private val analytics: Analytics by inject()
    private val currencyPrefs: CurrencyPrefs by inject()
    private val settingsDataManager: SettingsDataManager by scopedInject()

    private val compositeDisposable = CompositeDisposable()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_simple_buy_intro)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setupToolbar(R.string.simple_buy_intro_title)
        skip_simple_buy.setOnClickListener {
            analytics.logEvent(SimpleBuyAnalytics.SKIP_ALREADY_HAVE_CRYPTO)

            val updateCurrencyCompletable =
                if (currencyPrefs.selectedFiatCurrency.isNotEmpty()) {
                    Completable.complete()
                } else {
                    settingsDataManager.updateFiatUnit(currencyPrefs.defaultFiatCurrency).ignoreElements()
                }

            compositeDisposable += updateCurrencyCompletable.observeOn(AndroidSchedulers.mainThread()).subscribeBy({}, {
                navigator().exitSimpleBuyFlow()
            })
        }
        analytics.logEvent(SimpleBuyAnalytics.INTRO_SCREEN_SHOW)
        buy_crypto_now.setOnClickListener {
            analytics.logEvent(SimpleBuyAnalytics.I_WANT_TO_BUY_CRYPTO_BUTTON_CLICKED)
            compositeDisposable += nabuToken.fetchNabuToken()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    showLoadingState()
                }
                .subscribeBy(
                    onSuccess = {
                        simpleBuyPrefs.clearState()
                        navigator().goToCurrencySelection()
                    },
                    onError = {
                        showError()
                        analytics.logEvent(SimpleBuyAnalytics.I_WANT_TO_BUY_CRYPTO_ERROR)
                    }
                )
        }
    }

    private fun showError() {
        buy_crypto_now.visible()
        progress.gone()
        ErrorSlidingBottomDialog.newInstance(ErrorDialogData(
            resources.getString(R.string.ops),
            resources.getString(R.string.something_went_wrong_try_again),
            resources.getString(R.string.ok_cap)))
            .show(childFragmentManager, "BOTTOM_SHEET")
    }

    private fun showLoadingState() {
        buy_crypto_now.gone()
        progress.visible()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun navigator(): SimpleBuyNavigator =
        (activity as? SimpleBuyNavigator) ?: throw IllegalStateException("Parent must implement SimpleBuyNavigator")

    override fun onBackPressed(): Boolean = true
}