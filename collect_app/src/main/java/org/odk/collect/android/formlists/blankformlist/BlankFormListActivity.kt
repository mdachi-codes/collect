package org.odk.collect.android.formlists.blankformlist

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import org.odk.collect.android.R
import org.odk.collect.android.activities.FormMapActivity
import org.odk.collect.android.formmanagement.FormFillingIntentFactory
import org.odk.collect.android.injection.DaggerUtils
import org.odk.collect.android.preferences.dialogs.ServerAuthDialogFragment
import org.odk.collect.androidshared.network.NetworkStateProvider
import org.odk.collect.androidshared.ui.DialogFragmentUtils
import org.odk.collect.androidshared.ui.SnackbarUtils
import org.odk.collect.permissions.PermissionListener
import org.odk.collect.permissions.PermissionsProvider
import org.odk.collect.strings.localization.LocalizedActivity
import javax.inject.Inject

class BlankFormListActivity : LocalizedActivity(), OnFormItemClickListener {

    @Inject
    lateinit var viewModelFactory: BlankFormListViewModel.Factory

    @Inject
    lateinit var networkStateProvider: NetworkStateProvider

    @Inject
    lateinit var permissionsProvider: PermissionsProvider

    private val viewModel: BlankFormListViewModel by viewModels { viewModelFactory }

    private val adapter: BlankFormListAdapter = BlankFormListAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DaggerUtils.getComponent(this).inject(this)
        setContentView(R.layout.activity_blank_form_list)
        title = getString(R.string.enter_data)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setLogo(R.drawable.pact_logo_medium);
        setSupportActionBar(toolbar)

        val menuProvider = BlankFormListMenuProvider(this, viewModel, networkStateProvider)
        addMenuProvider(menuProvider, this)

        findViewById<RecyclerView>(R.id.form_list).adapter = adapter

        initObservers()
    }

    override fun onFormClick(formUri: Uri) {
        if (Intent.ACTION_PICK == intent.action) {
            // caller is waiting on a picked form
            setResult(RESULT_OK, Intent().setData(formUri))
        } else {
            // caller wants to view/edit a form, so launch FormFillingActivity
            startActivity(FormFillingIntentFactory.newInstanceIntent(this, formUri))
        }
        finish()
    }

    override fun onMapButtonClick(id: Long) {
        permissionsProvider.requestEnabledLocationPermissions(
            this,
            object : PermissionListener {
                override fun granted() {
                    startActivity(
                        Intent(this@BlankFormListActivity, FormMapActivity::class.java).also {
                            it.putExtra(FormMapActivity.EXTRA_FORM_ID, id)
                        }
                    )
                }
            }
        )
    }

    private fun initObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            findViewById<ProgressBar>(R.id.progressBar).visibility =
                if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.syncResult.observe(this) { result ->
            if (!result.isConsumed()) {
                SnackbarUtils.showShortSnackbar(findViewById(R.id.form_list), result.value)
            }
        }

        viewModel.formsToDisplay.observe(this) { forms ->
            forms?.let {
                findViewById<RecyclerView>(R.id.form_list).visibility =
                    if (forms.isEmpty()) View.GONE else View.VISIBLE

                findViewById<TextView>(R.id.empty_list_message).visibility =
                    if (forms.isEmpty()) View.VISIBLE else View.GONE

                adapter.setData(forms)
            }
        }

        viewModel.isAuthenticationRequired().observe(this) { authenticationRequired ->
            if (authenticationRequired) {
                DialogFragmentUtils.showIfNotShowing(
                    ServerAuthDialogFragment::class.java,
                    supportFragmentManager
                )
            } else {
                DialogFragmentUtils.dismissDialog(
                    ServerAuthDialogFragment::class.java,
                    supportFragmentManager
                )
            }
        }
    }
}
